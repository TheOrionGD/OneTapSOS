package com.sosence.app

import android.Manifest
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sosence.app.data.AppDatabaseHelper
import com.sosence.app.data.SosEventModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : BaseActivity() {

    private var countDownTimer: CountDownTimer? = null
    private val holdDurationMillis = 3000L

    private var volumeCountDownTimer: CountDownTimer? = null
    private var isVolumeKeyDown = false

    private var sosCountdownTimer: CountDownTimer? = null
    private var sosCountdownDialog: AlertDialog? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var legacyDbHelper: ContactsDatabaseHelper
    private lateinit var appDbHelper: AppDatabaseHelper

    private var isFallDetectionEnabled = false
    private lateinit var tvFallDetectionStatus: TextView

    private var hasBatteryAlertFired = false
    private var batteryAlertTimer: CountDownTimer? = null
    private var vibrator: Vibrator? = null

    private lateinit var cardImSafe: CardView
    private lateinit var tvImSafeStatus: TextView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false

        if (!locationGranted || !smsGranted) {
            Toast.makeText(this, "Location & SMS permissions are needed for SOS to work", Toast.LENGTH_LONG).show()
        } else {
            showLocationConsentDialog()
        }
    }

    private val requestActivityRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isFallDetectionEnabled = true
            startFallDetectionService()
            updateFallDetectionStatusText()
            Toast.makeText(this, "Fall Detection Enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied. Fall Detection cannot be enabled.", Toast.LENGTH_LONG).show()
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED && appSettings.isBatteryAlertEnabled) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                if (level > 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale.toFloat()
                    if (batteryPct <= 5.0f && !isCharging) {
                        if (!hasBatteryAlertFired) {
                            hasBatteryAlertFired = true
                            show5PercentBatteryAlert()
                        }
                    } else if (batteryPct > 6.0f || isCharging) {
                        hasBatteryAlertFired = false
                    }
                }
            }
        }
    }

    private var glowPulseAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        legacyDbHelper = ContactsDatabaseHelper(this)
        appDbHelper = AppDatabaseHelper(this)

        initVibrator()
        requestNeededPermissions()

        val scrollView = findViewById<View>(android.R.id.content)
        scrollView.alpha = 0f
        scrollView.animate()
            .alpha(1f)
            .setDuration(350)
            .setStartDelay(80)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        val glowRing = findViewById<View>(R.id.viewGlowRing)
        glowPulseAnimator = ObjectAnimator.ofFloat(glowRing, "alpha", 0.35f, 1f).apply {
            duration = 900
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            start()
        }

        val btnSettings = findViewById<TextView>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            btnSettings.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).withEndAction {
                btnSettings.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()
            showSettingsDialog()
        }

        val sosButton = findViewById<FrameLayout>(R.id.frameSOS)
        sosButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(100).start()
                    startHoldTimer()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                        .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                        .start()
                    cancelHoldTimer()
                    true
                }
                else -> false
            }
        }

        val layoutFeatures = findViewById<LinearLayout>(R.id.layoutFeatures)

        val safeMapCard = layoutFeatures.getChildAt(0) as CardView
        safeMapCard.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val aiChatCard = layoutFeatures.getChildAt(1) as CardView
        aiChatCard.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val vaultCard = layoutFeatures.getChildAt(2) as CardView
        vaultCard.setOnClickListener {
            startActivity(Intent(this, TrustedContactsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        tvFallDetectionStatus = findViewById(R.id.tvFallDetectionStatus)
        val cardFallDetection = findViewById<CardView>(R.id.cardFallDetection)
        cardFallDetection.setOnClickListener {
            showFallDetectionDialog()
        }

        cardImSafe = findViewById(R.id.cardImSafe)
        tvImSafeStatus = findViewById(R.id.tvImSafeStatus)
        cardImSafe.setOnClickListener {
            sendImSafeUpdate()
        }

        findViewById<CardView?>(R.id.cardAllHub)?.setOnClickListener {
            startActivity(Intent(this, SafetyDashboardActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        animateFeatureCardsEntrance(layoutFeatures)

        updateFallDetectionStatusText()
        updateImSafeCardState()
    }

    override fun onResume() {
        super.onResume()
        updateImSafeCardState()
    }

    private fun updateImSafeCardState() {
        val isActive = appSettings.isSosActive
        if (isActive) {
            tvImSafeStatus.text = "Send ✅"
            tvImSafeStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_teal))
            cardImSafe.alpha = 1.0f
        } else {
            tvImSafeStatus.text = "No active SOS"
            tvImSafeStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            cardImSafe.alpha = 0.6f
        }
    }

    private fun animateFeatureCardsEntrance(container: LinearLayout) {
        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.alpha = 0f
            child.postDelayed({
                child.alpha = 1f
                child.startAnimation(anim)
            }, (i * 80).toLong())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        glowPulseAnimator?.cancel()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {}
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun show5PercentBatteryAlert() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_battery_alert, null)
        val btnDismiss = dialogView.findViewById<CardView>(R.id.btnDismissBatteryAlert)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        startVibration15Seconds()

        btnDismiss.setOnClickListener {
            stopVibration()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startVibration15Seconds() {
        stopVibration()
        val pattern = longArrayOf(0, 500, 200, 500, 200)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        batteryAlertTimer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                stopVibration()
            }
        }.start()
    }

    private fun stopVibration() {
        batteryAlertTimer?.cancel()
        vibrator?.cancel()
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)

        val switchBattery = dialogView.findViewById<SwitchCompat>(R.id.switchBatteryAlert)
        val rb5s = dialogView.findViewById<RadioButton>(R.id.rbTimer5s)
        val rb15s = dialogView.findViewById<RadioButton>(R.id.rbTimer15s)
        val etMessage = dialogView.findViewById<EditText>(R.id.etCustomSafeMessage)
        val btnCancel = dialogView.findViewById<CardView>(R.id.btnCancelSettings)
        val btnSave = dialogView.findViewById<CardView>(R.id.btnSaveSettings)

        switchBattery.isChecked = appSettings.isBatteryAlertEnabled
        if (appSettings.sosTimerSeconds == 15) {
            rb15s.isChecked = true
        } else {
            rb5s.isChecked = true
        }
        etMessage.setText(appSettings.customSafeMessage)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            appSettings.isBatteryAlertEnabled = switchBattery.isChecked
            appSettings.sosTimerSeconds = if (rb15s.isChecked) 15 else 5
            val customMsg = etMessage.text.toString().trim()
            if (customMsg.isNotEmpty()) {
                appSettings.customSafeMessage = customMsg
            }
            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showFallDetectionDialog() {
        val message = if (isFallDetectionEnabled) {
            "Fall Detection is currently ENABLED. Do you want to disable it?"
        } else {
            "Fall Detection is currently DISABLED. Do you want to enable it?"
        }

        val actionText = if (isFallDetectionEnabled) "Disable" else "Enable"

        AlertDialog.Builder(this)
            .setTitle("Fall Detection")
            .setMessage(message)
            .setPositiveButton(actionText) { _, _ ->
                if (isFallDetectionEnabled) {
                    isFallDetectionEnabled = false
                    stopFallDetectionService()
                    Toast.makeText(this, "Fall Detection Disabled", Toast.LENGTH_SHORT).show()
                    updateFallDetectionStatusText()
                } else {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                        == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    ) {
                        isFallDetectionEnabled = true
                        startFallDetectionService()
                        Toast.makeText(this, "Fall Detection Enabled", Toast.LENGTH_SHORT).show()
                        updateFallDetectionStatusText()
                    } else {
                        requestActivityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateFallDetectionStatusText() {
        if (isFallDetectionEnabled) {
            tvFallDetectionStatus.text = "Enabled"
            tvFallDetectionStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_teal))
        } else {
            tvFallDetectionStatus.text = "Disabled"
            tvFallDetectionStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
        }
    }

    private fun startFallDetectionService() {
        try {
            val serviceIntent = Intent(this, FallDetectionService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopFallDetectionService() {
        try {
            val serviceIntent = Intent(this, FallDetectionService::class.java)
            stopService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestNeededPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun showLocationConsentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_location_permission, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnAllow = dialogView.findViewById<View>(R.id.btnAllow)
        val btnNotNow = dialogView.findViewById<View>(R.id.btnNotNow)

        btnAllow.setOnClickListener {
            Toast.makeText(this, "Location access enabled for this session", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnNotNow.setOnClickListener {
            Toast.makeText(this, "SOS may not work accurately without location", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startHoldTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(holdDurationMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                triggerSOS()
            }
        }.start()
    }

    private fun cancelHoldTimer() {
        countDownTimer?.cancel()
    }

    private fun triggerSOS() {
        val contacts = legacyDbHelper.getAllContacts()
        if (contacts.isEmpty()) {
            Toast.makeText(this, "No trusted contacts added! Add contacts first.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, TrustedContactsActivity::class.java))
            return
        }

        showSosCountdownDialog()
    }

    private fun showSosCountdownDialog() {
        val durationSeconds = appSettings.sosTimerSeconds
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sos_countdown, null)
        val tvCountdownNumber = dialogView.findViewById<TextView>(R.id.tvCountdownNumber)
        val btnCancelSOS = dialogView.findViewById<CardView>(R.id.btnCancelSOS)

        tvCountdownNumber.text = durationSeconds.toString()

        sosCountdownDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        sosCountdownDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelSOS.setOnClickListener {
            sosCountdownTimer?.cancel()
            sosCountdownDialog?.dismiss()
            Toast.makeText(this, "SOS Cancelled", Toast.LENGTH_SHORT).show()
        }

        sosCountdownDialog?.show()

        sosCountdownTimer?.cancel()
        sosCountdownTimer = object : CountDownTimer(durationSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                tvCountdownNumber.text = secondsLeft.toString()
            }

            override fun onFinish() {
                sosCountdownDialog?.dismiss()
                executeSendSOS()
            }
        }.start()
    }

    private fun executeSendSOS() {
        Toast.makeText(this, "🚨 SOS Sending! Getting location...", Toast.LENGTH_SHORT).show()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val locationRequest = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                fusedLocationClient.getCurrentLocation(locationRequest, null)
                    .addOnSuccessListener { location: Location? -> sendEmergencyAlert(location) }
                    .addOnFailureListener { sendEmergencyAlert(null) }
            } catch (e: SecurityException) {
                sendEmergencyAlert(null)
            }
        } else {
            sendEmergencyAlert(null)
        }
    }

    private fun buildSosMessage(location: Location?): String {
        val mapsLink = if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else "Location unavailable"
        val timeStamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        return buildString {
            appendLine("🚨 SOS ALERT")
            appendLine()
            appendLine("I need immediate assistance.")
            appendLine()
            appendLine("Location:")
            appendLine(mapsLink)
            appendLine()
            appendLine("Time: $timeStamp")
            appendLine()
            append("Please respond as soon as possible.")
        }
    }

    private fun buildImSafeMessage(location: Location?): String {
        val customMsg = appSettings.customSafeMessage
        val mapsLink = if (location != null) "\n\nCurrent location: https://maps.google.com/?q=${location.latitude},${location.longitude}" else ""
        return "$customMsg$mapsLink"
    }

    private fun sendEmergencyAlert(location: Location?) {
        val defaultMessage = buildSosMessage(location)
        val contacts = legacyDbHelper.getAllContacts()
        if (contacts.isEmpty()) {
            Toast.makeText(this, "No trusted contacts added.", Toast.LENGTH_LONG).show()
            return
        }

        val mapsLink = if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else "Location unavailable"
        val timeStamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val successfulRecipients = mutableListOf<String>()
        var sentCount = 0
        for (contact in contacts) {
            if (contact.phone.isNotEmpty()) {
                val contactMessage = if (contact.customMessage.isNotBlank()) {
                    "${contact.customMessage}\n\n🚨 SOS ALERT!\n📍 Location: $mapsLink\n🕒 Time: $timeStamp"
                } else {
                    defaultMessage
                }
                val success = sendSms(contact.phone, contactMessage)
                sentCount++
                if (success) successfulRecipients.add(contact.phone)
            }
        }

        appSettings.isSosActive = true
        appSettings.lastSosTimestamp = System.currentTimeMillis()
        appSettings.sosRecipients = successfulRecipients

        appDbHelper.recordSosEvent(SosEventModel(
            timestamp = System.currentTimeMillis(),
            latitude = location?.latitude ?: 0.0,
            longitude = location?.longitude ?: 0.0,
            locationName = if (location != null) "GPS Fix" else "Unknown",
            message = defaultMessage,
            recipientsCount = sentCount
        ))

        updateImSafeCardState()
        Toast.makeText(this, "✅ SOS alert sent to $sentCount trusted contact(s)!", Toast.LENGTH_LONG).show()

        startActivity(Intent(this, SOSActiveActivity::class.java))
    }


    private fun sendImSafeUpdate() {
        if (!appSettings.isSosActive) {
            Toast.makeText(this, "No active SOS session.", Toast.LENGTH_LONG).show()
            return
        }

        val recipients = appSettings.sosRecipients
        val phoneNumbers = if (recipients.isNotEmpty()) recipients else legacyDbHelper.getAllContacts().map { it.phone }
        fetchLocationAndSendImSafe(phoneNumbers)
    }

    private fun fetchLocationAndSendImSafe(phoneNumbers: List<String>) {
        try {
            val locationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            fusedLocationClient.getCurrentLocation(locationRequest, null)
                .addOnSuccessListener { location: Location? -> executeSendImSafe(location, phoneNumbers) }
                .addOnFailureListener { executeSendImSafe(null, phoneNumbers) }
        } catch (e: SecurityException) {
            executeSendImSafe(null, phoneNumbers)
        }
    }

    private fun executeSendImSafe(location: Location?, phoneNumbers: List<String>) {
        val message = buildImSafeMessage(location)
        var sentCount = 0
        for (phone in phoneNumbers) {
            if (phone.isNotEmpty()) {
                sendSms(phone, message)
                sentCount++
            }
        }

        appSettings.isSosActive = false
        appSettings.sosRecipients = emptyList()
        updateImSafeCardState()

        Toast.makeText(this, "✅ 'I'm Safe' message sent to $sentCount contact(s)!", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, SOSResolvedActivity::class.java))
    }

    private fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val formattedNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(formattedNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(formattedNumber, null, message, null, null)
            }
            true
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS to $phoneNumber", Toast.LENGTH_SHORT).show()
            false
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN && !isVolumeKeyDown) {
            isVolumeKeyDown = true
            startVolumeHoldTimer()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolumeKeyDown = false
            volumeCountDownTimer?.cancel()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun startVolumeHoldTimer() {
        volumeCountDownTimer?.cancel()
        volumeCountDownTimer = object : CountDownTimer(holdDurationMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                triggerSOS()
            }
        }.start()
    }
}
