package com.sosence.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.sosence.app.data.AppDatabaseHelper
import com.sosence.app.data.SosEventModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SafeMaps/SOS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.sosence.app.SEND_SOS") {
            val isFall = intent.getBooleanExtra("IS_FALL", false)
            Log.i(TAG, "[SOS] Background SOS broadcast received (isFall=$isFall)")
            Toast.makeText(context, if (isFall) "⚠️ Fall SOS Triggered!" else "🚨 SOS Triggered!", Toast.LENGTH_LONG).show()
            triggerSOSFromBackground(context, isFall)
        }
    }

    private fun triggerSOSFromBackground(context: Context, isFall: Boolean) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                sendAlertToContacts(context, location, isFall)
            }.addOnFailureListener {
                sendAlertToContacts(context, null, isFall)
            }
        } catch (e: SecurityException) {
            sendAlertToContacts(context, null, isFall)
        }
    }

    private fun sendAlertToContacts(
        context: Context,
        location: Location?,
        isFall: Boolean
    ) {
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val liveTrackingUrl = com.sosence.app.utils.LiveLocationPublisher.buildLiveTrackingUrl(context, location)
        val mapsLink = if (location != null) "https://maps.google.com/?q=$lat,$lng" else "Location unavailable"
        val timeStamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date())

        val message = if (isFall) {
            buildString {
                appendLine("⚠️ FALL DETECTED (EMERGENCY)")
                appendLine("A high-impact fall has been detected. I may need immediate emergency assistance!")
                appendLine()
                appendLine("🔴 LIVE MOVEMENT TRACKER (Watch My Real-Time Path):")
                appendLine(liveTrackingUrl)
                appendLine()
                appendLine("📍 Current GPS Pin:")
                appendLine(mapsLink)
                appendLine()
                appendLine("🕒 Time: $timeStamp")
                append("⚡ Tap the Live Tracker link to follow my real-time moving location and route on your map.")
            }
        } else {
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
                appendLine("🕒 Time: $timeStamp")
                append("⚡ Tap the Live Tracker link to follow my real-time moving location and route on your map.")
            }
        }

        val appDbHelper = AppDatabaseHelper(context)
        val contacts = appDbHelper.getAllContacts().filter { it.isEnabled }

        val successfulRecipients = mutableListOf<String>()
        var count = 0
        for (contact in contacts) {
            if (contact.phone.isNotEmpty()) {
                val contactMessage = if (contact.customMessage.isNotBlank() && !isFall) {
                    "${contact.customMessage}\n\n$message"
                } else {
                    message
                }
                val success = sendSms(context, contact.phone, contactMessage)
                count++
                if (success) {
                    successfulRecipients.add(contact.phone)
                }
            }
        }

        val appSettings = AppSettings(context)
        appSettings.isSosActive = true
        appSettings.lastSosTimestamp = System.currentTimeMillis()
        appSettings.sosRecipients = if (successfulRecipients.isNotEmpty()) successfulRecipients else contacts.map { it.phone }

        // Start background live location streaming
        com.sosence.app.utils.LiveLocationPublisher.publishLocation(context, location, isSos = true)
        try {
            val serviceIntent = Intent(context, SOSForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Persist authoritative SOS record in Room/SQLite database
        val triggerType = if (isFall) "FALL_DETECTION" else "BACKGROUND_SOS"
        val locationSummary = if (location != null) "GPS: %.4f, %.4f".format(lat, lng) else "Unknown"
        val eventId = appDbHelper.recordSosEvent(
            SosEventModel(
                timestamp = System.currentTimeMillis(),
                formattedTime = timeStamp,
                latitude = lat,
                longitude = lng,
                locationName = locationSummary,
                message = message,
                recipientsCount = contacts.size,
                isResolved = false,
                triggerType = triggerType
            )
        )
        Log.i(TAG, "[SOS] Background SOS record created: id=$eventId, trigger=$triggerType, recipients=${contacts.size}")

        // Show Persistent Emergency SOS notification
        SOSNotificationManager.showEmergencySosNotification(
            context,
            locationSummary,
            contacts.size
        )

        Toast.makeText(context, "✅ SOS alert sent to ${contacts.size} contact(s)!", Toast.LENGTH_LONG).show()
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String): Boolean {
        return try {
            val formattedNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
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
            Log.e(TAG, "[SOS] Failed to send SMS to $phoneNumber", e)
            false
        }
    }
}
