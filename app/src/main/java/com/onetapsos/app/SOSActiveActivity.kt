package com.onetapsos.app

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.onetapsos.app.data.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSActiveActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private var sosEventId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_active)

        dbHelper = AppDatabaseHelper(this)
        sosEventId = intent.getLongExtra("SOS_EVENT_ID", -1L)

        val tvActiveTime = findViewById<TextView>(R.id.tvActiveTime)
        val tvLocationValue = findViewById<TextView>(R.id.tvLocationValue)
        val tvContactsValue = findViewById<TextView>(R.id.tvContactsValue)

        val timestamp = appSettings.lastSosTimestamp
        val timeStr = if (timestamp > 0) SimpleDateFormat("HH:mm:ss, dd MMM yyyy", Locale.getDefault()).format(Date(timestamp)) else "Just now"
        tvActiveTime.text = "Alert Sent: $timeStr"

        val recipients = appSettings.sosRecipients
        val contacts = dbHelper.getAllContacts()
        tvContactsValue.text = "${if (recipients.isNotEmpty()) recipients.size else contacts.size} Contact(s) Notified"

        val locations = dbHelper.getLocationHistory()
        if (locations.isNotEmpty()) {
            val loc = locations.first()
            tvLocationValue.text = "Lat: %.4f, Lng: %.4f".format(loc.latitude, loc.longitude)
        } else {
            tvLocationValue.text = "GPS High Accuracy Active"
        }

        findViewById<CardView>(R.id.btnLiveTracking).setOnClickListener {
            startActivity(Intent(this, LiveTrackingActivity::class.java))
        }

        findViewById<CardView>(R.id.btnViewMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        findViewById<CardView>(R.id.btnImSafe).setOnClickListener {
            executeImSafeFlow()
        }
    }

    private fun executeImSafeFlow() {
        Toast.makeText(this, "Sending 'I'm Safe' updates to contacts...", Toast.LENGTH_SHORT).show()
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val req = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
                fusedClient.getCurrentLocation(req, null)
                    .addOnSuccessListener { loc ->
                        if (!isFinishing && !isDestroyed) {
                            sendImSafeMessages(loc)
                        }
                    }
                    .addOnFailureListener {
                        if (!isFinishing && !isDestroyed) {
                            sendImSafeMessages(null)
                        }
                    }
            } catch (e: Exception) {
                if (!isFinishing && !isDestroyed) {
                    sendImSafeMessages(null)
                }
            }
        } else {
            if (!isFinishing && !isDestroyed) {
                sendImSafeMessages(null)
            }
        }
    }

    private fun sendImSafeMessages(location: Location?) {
        val customMsg = appSettings.customSafeMessage
        val mapLink = if (location != null) "\n\nCurrent location: https://maps.google.com/?q=${location.latitude},${location.longitude}" else ""
        val fullMessage = "$customMsg$mapLink"

        var recipients = appSettings.sosRecipients
        if (recipients.isEmpty()) {
            recipients = dbHelper.getAllContacts().map { it.phone }
        }

        var sentCount = 0
        for (phone in recipients) {
            if (phone.isNotEmpty()) {
                if (sendSms(phone, fullMessage)) sentCount++
            }
        }

        val resolutionTime = System.currentTimeMillis()
        if (sosEventId != -1L) {
            dbHelper.markSosResolved(sosEventId, resolutionTime)
        }
        appSettings.isSosActive = false
        appSettings.sosRecipients = emptyList()

        // Update phone notification
        SOSNotificationManager.cancelEmergencySosNotification(this)
        SOSNotificationManager.showSosResolvedNotification(this)

        Toast.makeText(this, "✅ 'I'm Safe' sent to $sentCount contact(s)", Toast.LENGTH_LONG).show()

        val intent = Intent(this, SOSResolvedActivity::class.java).apply {
            putExtra("RESOLUTION_TIME", resolutionTime)
            putExtra("RECIPIENTS_COUNT", sentCount)
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
}
