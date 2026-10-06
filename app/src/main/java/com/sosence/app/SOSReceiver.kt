package com.sosence.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.telephony.SmsManager
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.sosence.app.SEND_SOS") {
            val isFall = intent.getBooleanExtra("IS_FALL", false)
            Toast.makeText(context, "🚨 SOS Triggered!", Toast.LENGTH_LONG).show()
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
        val mapsLink = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Location unavailable"
        }
        val timeStamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val message = if (isFall) {
            buildString {
                appendLine("⚠️ FALL DETECTED")
                appendLine()
                appendLine("A possible fall has been detected. I may need help.")
                appendLine()
                appendLine("Location:")
                appendLine(mapsLink)
                appendLine()
                appendLine("Time: $timeStamp")
                appendLine()
                append("Please check on me as soon as possible.")
            }
        } else {
            buildString {
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

        val dbHelper = ContactsDatabaseHelper(context)
        val contacts = dbHelper.getAllContacts()

        if (contacts.isEmpty()) {
            Toast.makeText(context, "No trusted contacts added in local database", Toast.LENGTH_LONG).show()
        } else {
            val successfulRecipients = mutableListOf<String>()
            var count = 0
            for (contact in contacts) {
                if (contact.phone.isNotEmpty()) {
                    val contactMessage = if (contact.customMessage.isNotBlank() && !isFall) {
                        "${contact.customMessage}\n\n🚨 SOS ALERT!\nLocation: $mapsLink\nTime: $timeStamp"
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
            if (count > 0) {
                val appSettings = AppSettings(context)
                appSettings.isSosActive = true
                appSettings.lastSosTimestamp = System.currentTimeMillis()
                appSettings.sosRecipients = successfulRecipients
                Toast.makeText(context, "✅ SOS alert sent to $count trusted contact(s)!", Toast.LENGTH_LONG).show()
            }
        }
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
            Toast.makeText(context, "Failed to send SMS to $phoneNumber", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
