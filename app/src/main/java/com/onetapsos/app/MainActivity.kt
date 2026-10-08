package com.onetapsos.app

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.onetapsos.app.data.AppDatabaseHelper
import com.onetapsos.app.data.SosEventModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : BaseActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var activeFragment: Fragment? = null

    val homeFragment = HomeFragment()
    val safetyFragment = SafetyFragment()
    val mapFragment = MapFragment()
    val contactsFragment = ContactsFragment()
    val moreFragment = MoreFragment()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var legacyDbHelper: ContactsDatabaseHelper
    private lateinit var appDbHelper: AppDatabaseHelper
    private var vibrator: Vibrator? = null

    private var volumeCountDownTimer: CountDownTimer? = null
    private var isVolumeKeyDown = false
    private val holdDurationMillis = 3000L

    private var sosCountdownTimer: CountDownTimer? = null
    private var sosCountdownDialog: AlertDialog? = null
    private var isFallDetectionEnabled = false

    private var hasBatteryAlertFired = false
    private var batteryAlertTimer: CountDownTimer? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false

        if (!locationGranted || !smsGranted) {
            Toast.makeText(this, "Location & SMS permissions are recommended for full SOS alerts", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestActivityRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            appSettings.isFallDetectionEnabled = true
            startFallDetectionService()
            Toast.makeText(this, "Fall Detection Enabled & Monitoring", Toast.LENGTH_SHORT).show()
            homeFragment.refreshDashboardState()
        } else {
            Toast.makeText(this, "Permission denied. Fall Detection cannot be enabled.", Toast.LENGTH_LONG).show()
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED && appSettings.isBatteryAlertEnabled) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                    if (level > 0 && scale > 0) {
                        val batteryPct = (level * 100) / scale.toFloat()
                        if (batteryPct in 1.0f..5.0f && !isCharging) {
                            if (!hasBatteryAlertFired) {
                                hasBatteryAlertFired = true
                                SOSNotificationManager.showLowBatteryAlertNotification(context, 5)
                                window.decorView.postDelayed({
                                    if (!isFinishing && !isDestroyed) {
                                        show5PercentBatteryAlert()
                                    }
                                }, 1500)
                            }
                        } else if (batteryPct > 6.0f || isCharging) {
                            hasBatteryAlertFired = false
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        legacyDbHelper = ContactsDatabaseHelper(this)
        appDbHelper = AppDatabaseHelper(this)
        initVibrator()

        setupBottomNavigation()

        window.decorView.post {
            if (!isFinishing && !isDestroyed) {
                requestNeededPermissions()
            }
        }

        // Handle target tab intent if provided
        val targetTab = intent.getIntExtra("TARGET_TAB", R.id.nav_home)
        if (targetTab != R.id.nav_home) {
            navigateToTab(targetTab)
        }

        if (appSettings.isFallDetectionEnabled && !FallDetectionService.isServiceRunning) {
            startFallDetectionService()
        }
    }

    private fun setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation)

        supportFragmentManager.beginTransaction().apply {
            add(R.id.containerMainTabs, moreFragment, "MORE").hide(moreFragment)
            add(R.id.containerMainTabs, contactsFragment, "CONTACTS").hide(contactsFragment)
            add(R.id.containerMainTabs, mapFragment, "MAP").hide(mapFragment)
            add(R.id.containerMainTabs, safetyFragment, "SAFETY").hide(safetyFragment)
            add(R.id.containerMainTabs, homeFragment, "HOME")
            commit()
        }
        activeFragment = homeFragment

        updateNavDynamicAppearance(R.id.nav_home)
        updateNavBadges()

        bottomNav.setOnItemSelectedListener { item ->
            updateNavDynamicAppearance(item.itemId)
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_safety -> {
                    switchFragment(safetyFragment)
                    true
                }
                R.id.nav_map -> {
                    switchFragment(mapFragment)
                    true
                }
                R.id.nav_contacts -> {
                    switchFragment(contactsFragment)
                    true
                }
                R.id.nav_more -> {
                    switchFragment(moreFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateNavDynamicAppearance(selectedItemId: Int) {
        try {
            val activeColor = when (selectedItemId) {
                R.id.nav_home -> {
                    if (appSettings.isSosActive) android.graphics.Color.parseColor("#E63946") else android.graphics.Color.parseColor("#00D9FF")
                }
                R.id.nav_safety -> android.graphics.Color.parseColor("#06D6A0")
                R.id.nav_map -> android.graphics.Color.parseColor("#4CC9F0")
                R.id.nav_contacts -> android.graphics.Color.parseColor("#B388FF")
                R.id.nav_more -> android.graphics.Color.parseColor("#FFD166")
                else -> android.graphics.Color.parseColor("#00D9FF")
            }

            val unselectedColor = android.graphics.Color.parseColor("#6C6C8A")
            val states = arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            )
            val colors = intArrayOf(activeColor, unselectedColor)
            val colorStateList = android.content.res.ColorStateList(states, colors)

            bottomNav.itemTextColor = colorStateList
            bottomNav.itemIconTintList = colorStateList

            // Dynamic translucent active indicator pill
            val pillColor = android.graphics.Color.argb(
                40,
                android.graphics.Color.red(activeColor),
                android.graphics.Color.green(activeColor),
                android.graphics.Color.blue(activeColor)
            )
            bottomNav.itemActiveIndicatorColor = android.content.res.ColorStateList.valueOf(pillColor)

            // Tactile micro-animation bounce on selected tab view
            val menuView = bottomNav.findViewById<android.view.View>(selectedItemId)
            menuView?.let { view ->
                view.scaleX = 0.88f
                view.scaleY = 0.88f
                view.animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(160)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                    .withEndAction {
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    }
                    .start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateNavBadges() {
        try {
            // 1. Contacts count badge
            val contactsCount = appDbHelper.getAllContacts().size
            val contactsBadge = bottomNav.getOrCreateBadge(R.id.nav_contacts)
            if (contactsCount > 0) {
                contactsBadge.isVisible = true
                contactsBadge.number = contactsCount
                contactsBadge.backgroundColor = android.graphics.Color.parseColor("#7209B7")
                contactsBadge.badgeTextColor = android.graphics.Color.WHITE
            } else {
                contactsBadge.isVisible = false
            }

            // 2. Emergency Active Badge on Home
            val homeBadge = bottomNav.getOrCreateBadge(R.id.nav_home)
            if (appSettings.isSosActive) {
                homeBadge.isVisible = true
                homeBadge.backgroundColor = android.graphics.Color.parseColor("#E63946")
                homeBadge.badgeTextColor = android.graphics.Color.WHITE
            } else {
                homeBadge.isVisible = false
            }

            // 3. Fall Detection / Safety Monitoring badge
            val safetyBadge = bottomNav.getOrCreateBadge(R.id.nav_safety)
            if (isFallDetectionEnabled) {
                safetyBadge.isVisible = true
                safetyBadge.backgroundColor = android.graphics.Color.parseColor("#06D6A0")
                safetyBadge.badgeTextColor = android.graphics.Color.BLACK
            } else {
                safetyBadge.isVisible = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun switchFragment(target: Fragment) {
        if (activeFragment == target) return
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            activeFragment?.let { hide(it) }
            show(target)
            commit()
        }
        activeFragment = target
    }

    fun navigateToTab(tabId: Int) {
        bottomNav.selectedItemId = tabId
        updateNavDynamicAppearance(tabId)
    }

    override fun onResume() {
        super.onResume()
        if (appSettings.isFallDetectionEnabled && !FallDetectionService.isServiceRunning) {
            startFallDetectionService()
        }
        homeFragment.refreshDashboardState()
        updateNavDynamicAppearance(bottomNav.selectedItemId)
        updateNavBadges()
    }

    override fun onStart() {
        super.onStart()
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeCountDownTimer?.cancel()
        sosCountdownTimer?.cancel()
        stopVibration()
        try {
            sosCountdownDialog?.dismiss()
        } catch (e: Exception) {}
    }

    override fun onBackPressed() {
        if (activeFragment != homeFragment) {
            navigateToTab(R.id.nav_home)
        } else {
            super.onBackPressed()
        }
    }

    // ==========================================
    // SOS WORKFLOW METHODS
    // ==========================================
    fun launchSosCountdownWorkflow() {
        val appContacts = appDbHelper.getAllContacts().filter { it.isEnabled }
        val contacts = if (appContacts.isNotEmpty()) {
            appContacts.map { Contact(it.name, it.phone, it.id, it.customMessage) }
        } else {
            legacyDbHelper.getAllContacts()
        }
        if (contacts.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ No Emergency Contacts")
                .setMessage("You haven't added any emergency contacts yet.\n\nAdd trusted contacts so they receive your emergency SOS SMS and live location, or dial 112 directly.")
                .setPositiveButton("Add Contacts") { _, _ ->
                    navigateToTab(R.id.nav_contacts)
                }
                .setNegativeButton("Call 112") { _, _ ->
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    startActivity(dialIntent)
                }
                .show()
            return
        }
        showSosCountdownDialog()
    }

    private fun showSosCountdownDialog() {
        if (isFinishing || isDestroyed) return
        try {
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
                    try {
                        sosCountdownDialog?.dismiss()
                    } catch (e: Exception) {}
                    executeSendSOS()
                }
            }.start()
        } catch (e: Exception) {
            executeSendSOS()
        }
    }

    fun executeSendSOS() {
        Toast.makeText(this, "🚨 SOS Triggered! Dispatching emergency alert...", Toast.LENGTH_SHORT).show()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val locationRequest = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                fusedLocationClient.getCurrentLocation(locationRequest, null)
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            sendEmergencyAlert(location)
                        } else {
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                sendEmergencyAlert(lastLoc)
                            }.addOnFailureListener {
                                sendEmergencyAlert(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            sendEmergencyAlert(lastLoc)
                        }.addOnFailureListener {
                            sendEmergencyAlert(null)
                        }
                    }
            } catch (e: SecurityException) {
                sendEmergencyAlert(null)
            }
        } else {
            sendEmergencyAlert(null)
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        val clean = phone.replace("[^0-9+]".toRegex(), "")
        return if (clean.startsWith("+")) {
            clean
        } else if (clean.length == 10) {
            "+91$clean"
        } else {
            clean
        }
    }

    private fun sendEmergencyAlert(location: Location?) {
        val defaultMessage = buildSosMessage(location)
        
        // Query both AppDatabaseHelper and legacy database to ensure no contact is missed
        val appContacts = appDbHelper.getAllContacts().filter { it.isEnabled }
        val contacts = if (appContacts.isNotEmpty()) {
            appContacts.map { Contact(it.name, it.phone, it.id, it.customMessage) }
        } else {
            legacyDbHelper.getAllContacts()
        }

        if (contacts.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ No Emergency Contacts Found")
                .setMessage("You haven't added any trusted emergency contacts yet.\n\nAdd contacts now to automatically broadcast emergency alerts, or call Emergency Services directly.")
                .setPositiveButton("Add Contacts") { _, _ ->
                    startActivity(Intent(this, AddTrustedContactActivity::class.java))
                }
                .setNegativeButton("Dial 112 (Emergency)") { _, _ ->
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    startActivity(dialIntent)
                }
                .show()
            return
        }

        val mapsLink = if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else "Location unavailable"
        val timeStamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val successfulRecipients = mutableListOf<String>()
        var sentCount = 0
        for (contact in contacts) {
            if (contact.phone.isNotBlank()) {
                val contactMessage = if (contact.customMessage.isNotBlank()) {
                    "${contact.customMessage}\n\n$defaultMessage"
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
        appSettings.sosRecipients = if (successfulRecipients.isNotEmpty()) successfulRecipients else contacts.map { it.phone }

        // Start background live location streaming
        com.onetapsos.app.utils.LiveLocationPublisher.publishLocation(this, location, isSos = true)
        try {
            val serviceIntent = Intent(this, SOSForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val locSummary = if (location != null) "GPS: %.4f, %.4f".format(location.latitude, location.longitude) else "Live GPS Active"
        val eventId = appDbHelper.recordSosEvent(
            SosEventModel(
                timestamp = System.currentTimeMillis(),
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                locationName = locSummary,
                message = defaultMessage,
                recipientsCount = sentCount,
                isResolved = false,
                triggerType = "MANUAL_SOS"
            )
        )

        // 1. Show Persistent System Emergency Notification
        SOSNotificationManager.showEmergencySosNotification(this, locSummary, sentCount)

        // 2. In-App Notification / Dialog message confirming emergency alert dispatched to the contacts
        val contactNames = contacts.joinToString("\n") { "• ${it.name} (${it.phone})" }
        AlertDialog.Builder(this)
            .setTitle("🚨 SOS Emergency Alert Dispatched")
            .setMessage("Emergency distress alert and live GPS broadcast sent to $sentCount contact(s):\n\n$contactNames\n\nStatus: $locSummary")
            .setPositiveButton("View Active Session") { _, _ ->
                val activeIntent = Intent(this, SOSActiveActivity::class.java).apply {
                    putExtra("SOS_EVENT_ID", eventId)
                }
                startActivity(activeIntent)
            }
            .setNegativeButton("OK", null)
            .show()

        homeFragment.refreshDashboardState()
        Toast.makeText(this, "🚨 SOS alert dispatched to $sentCount contact(s)!", Toast.LENGTH_LONG).show()
    }

    fun triggerImSafeResolution() {
        if (!appSettings.isSosActive) {
            Toast.makeText(this, "No active SOS session.", Toast.LENGTH_SHORT).show()
            return
        }

        val recipients = appSettings.sosRecipients
        val phoneNumbers = if (recipients.isNotEmpty()) {
            recipients
        } else {
            appDbHelper.getAllContacts().map { it.phone }.ifEmpty { legacyDbHelper.getAllContacts().map { it.phone } }
        }
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
            if (phone.isNotBlank()) {
                sendSms(phone, message)
                sentCount++
            }
        }

        val resolutionTime = System.currentTimeMillis()
        val finalCount = if (sentCount > 0) sentCount else if (phoneNumbers.isNotEmpty()) phoneNumbers.size else 1

        appSettings.isSosActive = false
        appSettings.sosRecipients = emptyList()

        appDbHelper.markLatestSosResolved(resolutionTime)

        SOSNotificationManager.cancelEmergencySosNotification(this)
        SOSNotificationManager.showSosResolvedNotification(this)

        homeFragment.refreshDashboardState()

        Toast.makeText(this, "✅ 'I'm Safe' message sent to $finalCount contact(s)!", Toast.LENGTH_LONG).show()
        val intent = Intent(this, SOSResolvedActivity::class.java).apply {
            putExtra("RESOLUTION_TIME", resolutionTime)
            putExtra("RECIPIENTS_COUNT", finalCount)
        }
        startActivity(intent)
    }

    private fun buildSosMessage(location: Location?): String {
        val timeStamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date())
        val liveTrackingUrl = com.onetapsos.app.utils.LiveLocationPublisher.buildLiveTrackingUrl(this, location)
        return if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            val mapsLink = "https://maps.google.com/?q=$lat,$lng"
            val accuracy = if (location.hasAccuracy()) "±%.0fm".format(location.accuracy) else "High"
            val speed = if (location.hasSpeed() && location.speed > 0) " • Speed: %.0f km/h".format(location.speed * 3.6f) else ""
            
            buildString {
                appendLine("🚨 SOS EMERGENCY BROADCAST")
                appendLine("I need immediate emergency assistance!")
                appendLine()
                appendLine("🔴 LIVE MOVEMENT TRACKER (Watch My Real-Time Path):")
                appendLine(liveTrackingUrl)
                appendLine()
                appendLine("📍 Current GPS Pin:")
                appendLine(mapsLink)
                appendLine()
                appendLine("🕒 Time: $timeStamp (GPS: $accuracy$speed)")
                append("⚡ Tap the Live Tracker link to follow my real-time moving location and route on your map.")
            }
        } else {
            buildString {
                appendLine("🚨 SOS EMERGENCY BROADCAST")
                appendLine("I need immediate emergency assistance!")
                appendLine()
                appendLine("🔴 LIVE TRACKER STREAM:")
                appendLine(liveTrackingUrl)
                appendLine()
                appendLine("📍 Location: Acquiring live GPS fix...")
                appendLine("🕒 Time: $timeStamp")
                append("Please call or respond immediately!")
            }
        }
    }

    private fun buildImSafeMessage(location: Location?): String {
        val customMsg = appSettings.customSafeMessage
        val mapsLink = if (location != null) "\n\nCurrent location: https://maps.google.com/?q=${location.latitude},${location.longitude}" else ""
        return "$customMsg$mapsLink"
    }

    private fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val formattedNumber = formatPhoneNumber(phoneNumber)
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java) ?: @Suppress("DEPRECATION") SmsManager.getDefault()
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
            Log.i("MainActivity", "Direct background SMS successfully sent to $formattedNumber via SmsManager")
            true
        } catch (e: Exception) {
            Log.e("MainActivity", "sendSms failed for $phoneNumber: ${e.message}", e)
            false
        }
    }

    // ==========================================
    // HARDWARE VOLUME KEY HOLD SOS TRIGGER
    // ==========================================
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
                launchSosCountdownWorkflow()
            }
        }.start()
    }

    // ==========================================
    // FALL DETECTION & BATTERY HELPERS
    // ==========================================
    fun showFallDetectionDialog() {
        if (isFinishing || isDestroyed) return
        try {
            val isMonitoring = FallDetectionService.isServiceRunning || appSettings.isFallDetectionEnabled
            val message = if (isMonitoring) {
                "Fall Detection is currently ACTIVE & MONITORING sensors.\n\nDo you want to disable it?"
            } else {
                "Fall Detection provides 24/7 background acceleration & impact monitoring.\n\nDo you want to enable it?"
            }
            val actionText = if (isMonitoring) "Disable" else "Enable"

            AlertDialog.Builder(this)
                .setTitle("Fall Detection Monitoring")
                .setMessage(message)
                .setPositiveButton(actionText) { _, _ ->
                    if (isMonitoring) {
                        appSettings.isFallDetectionEnabled = false
                        stopFallDetectionService()
                        Toast.makeText(this, "Fall Detection Disabled", Toast.LENGTH_SHORT).show()
                        homeFragment.refreshDashboardState()
                    } else {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                            == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                        ) {
                            appSettings.isFallDetectionEnabled = true
                            startFallDetectionService()
                            Toast.makeText(this, "Fall Detection Enabled & Monitoring", Toast.LENGTH_SHORT).show()
                            homeFragment.refreshDashboardState()
                        } else {
                            requestActivityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startFallDetectionService() {
        try {
            val serviceIntent = Intent(this, FallDetectionService::class.java).apply {
                action = FallDetectionService.ACTION_START
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {}
    }

    private fun stopFallDetectionService() {
        try {
            val serviceIntent = Intent(this, FallDetectionService::class.java).apply {
                action = FallDetectionService.ACTION_STOP
            }
            startService(serviceIntent)
        } catch (e: Exception) {}
    }

    private fun show5PercentBatteryAlert() {
        if (isFinishing || isDestroyed) return
        try {
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
        } catch (e: Exception) {}
    }

    private fun initVibrator() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {}
    }

    private fun startVibration15Seconds() {
        stopVibration()
        try {
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
        } catch (e: Exception) {}
    }

    private fun stopVibration() {
        try {
            batteryAlertTimer?.cancel()
            vibrator?.cancel()
        } catch (e: Exception) {}
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.SEND_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CALL_PHONE)
        }
        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }
}
