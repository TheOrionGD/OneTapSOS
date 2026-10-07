package com.sosence.app

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sosence.app.data.AppDatabaseHelper
import com.sosence.app.data.SosEventModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSCountdownActivity : BaseActivity() {

    private var countdownTimer: CountDownTimer? = null
    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_countdown)

        dbHelper = AppDatabaseHelper(this)

        val tvCountdownNumber = findViewById<TextView>(R.id.tvCountdownNumber)
        val btnCancelCountdown = findViewById<CardView>(R.id.btnCancelCountdown)

        val durationSeconds = appSettings.sosTimerSeconds
        tvCountdownNumber.text = durationSeconds.toString()

        btnCancelCountdown.setOnClickListener {
            countdownTimer?.cancel()
            Toast.makeText(this, "SOS Alert Cancelled", Toast.LENGTH_SHORT).show()
            finish()
        }

        countdownTimer = object : CountDownTimer(durationSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secLeft = (millisUntilFinished / 1000).toInt() + 1
                tvCountdownNumber.text = secLeft.toString()
            }

            override fun onFinish() {
                executeSOSAlert()
            }
        }.start()
    }

    private fun executeSOSAlert() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val req = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
                fusedClient.getCurrentLocation(req, null)
                    .addOnSuccessListener { loc ->
                        if (!isFinishing && !isDestroyed) {
                            sendEmergencyMessages(loc)
                        }
                    }
                    .addOnFailureListener {
                        if (!isFinishing && !isDestroyed) {
                            sendEmergencyMessages(null)
                        }
                    }
            } catch (e: Exception) {
                if (!isFinishing && !isDestroyed) {
                    sendEmergencyMessages(null)
                }
            }
        } else {
            if (!isFinishing && !isDestroyed) {
                sendEmergencyMessages(null)
            }
        }
    }

    private fun sendEmergencyMessages(location: Location?) {
        val contacts = dbHelper.getAllContacts().filter { it.isEnabled }
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val liveTrackingUrl = com.sosence.app.utils.LiveLocationPublisher.buildLiveTrackingUrl(this, location)
        val mapLink = if (location != null) "https://maps.google.com/?q=$lat,$lng" else "Acquiring GPS..."
        val timeStr = SimpleDateFormat("HH:mm:ss, dd MMM", Locale.getDefault()).format(Date())
        
        val defaultMessage = """
            🚨 SOS EMERGENCY BROADCAST
            I need immediate emergency assistance!
            
            🔴 LIVE MOVEMENT TRACKER (Watch My Real-Time Path):
            $liveTrackingUrl
            
            📍 Current GPS Pin:
            $mapLink
            
            🕒 Time: $timeStr
            ⚡ Tap the Live Tracker link to follow my real-time moving location and route on your map.
        """.trimIndent()

        val sentPhones = mutableListOf<String>()
        for (c in contacts) {
            if (c.phone.isNotEmpty()) {
                val contactMsg = if (c.customMessage.isNotBlank()) {
                    "${c.customMessage}\n\n$defaultMessage"
                } else {
                    defaultMessage
                }
                val ok = sendSms(c.phone, contactMsg)
                if (ok) sentPhones.add(c.phone)
            }
        }

        // Save session data
        appSettings.isSosActive = true
        appSettings.lastSosTimestamp = System.currentTimeMillis()
        appSettings.sosRecipients = if (sentPhones.isNotEmpty()) sentPhones else contacts.map { it.phone }

        // Start background live location streaming
        com.sosence.app.utils.LiveLocationPublisher.publishLocation(this, location, isSos = true)
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

        val locationSummary = if (location != null) "GPS: %.4f, %.4f".format(lat, lng) else "Live GPS Active"
        val eventId = dbHelper.recordSosEvent(SosEventModel(
            timestamp = System.currentTimeMillis(),
            latitude = lat,
            longitude = lng,
            locationName = locationSummary,
            message = defaultMessage,
            recipientsCount = contacts.size,
            isResolved = false,
            triggerType = "MANUAL_SOS"
        ))

        SOSNotificationManager.showEmergencySosNotification(this, locationSummary, contacts.size)

        Toast.makeText(this, "🚨 SOS Alert Sent to ${contacts.size} contact(s)!", Toast.LENGTH_LONG).show()

        val intent = Intent(this, SOSActiveActivity::class.java).apply {
            putExtra("SOS_EVENT_ID", eventId)
        }
        startActivity(intent)
        finish()
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

    private fun sendSms(phone: String, msg: String): Boolean {
        return try {
            val formatted = formatPhoneNumber(phone)
            val smsMgr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsMgr.divideMessage(msg)
            if (parts.size > 1) {
                smsMgr.sendMultipartTextMessage(formatted, null, parts, null, null)
            } else {
                smsMgr.sendTextMessage(formatted, null, msg, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }
}
