package com.onetapsos.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.telephony.SmsManager
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.onetapsos.app.data.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RESOLVE_SOS = "com.onetapsos.app.ACTION_RESOLVE_SOS"
        const val ACTION_CANCEL_FALL = "com.onetapsos.app.ACTION_CANCEL_FALL"
        const val ACTION_CONFIRM_CHECK_IN = "com.onetapsos.app.ACTION_CONFIRM_CHECK_IN"
        const val ACTION_EXTEND_TIMER = "com.onetapsos.app.ACTION_EXTEND_TIMER"
        const val ACTION_STOP_JOURNEY = "com.onetapsos.app.ACTION_STOP_JOURNEY"
        const val ACTION_TRIGGER_INSTANT_SOS = "com.onetapsos.app.ACTION_TRIGGER_INSTANT_SOS"
        const val ACTION_ANSWER_FAKE_CALL = "com.onetapsos.app.ACTION_ANSWER_FAKE_CALL"
        const val ACTION_DECLINE_FAKE_CALL = "com.onetapsos.app.ACTION_DECLINE_FAKE_CALL"
        const val ACTION_SEND_BATTERY_SMS = "com.onetapsos.app.ACTION_SEND_BATTERY_SMS"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_RESOLVE_SOS -> handleResolveSos(context)
            ACTION_CANCEL_FALL -> handleCancelFall(context)
            ACTION_CONFIRM_CHECK_IN -> handleConfirmCheckIn(context)
            ACTION_EXTEND_TIMER -> handleExtendTimer(context)
            ACTION_STOP_JOURNEY -> handleStopJourney(context)
            ACTION_TRIGGER_INSTANT_SOS -> handleTriggerInstantSos(context)
            ACTION_ANSWER_FAKE_CALL -> handleAnswerFakeCall(context)
            ACTION_DECLINE_FAKE_CALL -> handleDeclineFakeCall(context)
            ACTION_SEND_BATTERY_SMS -> handleSendBatterySms(context)
        }
    }

    private fun handleResolveSos(context: Context) {
        val appSettings = AppSettings(context)
        appSettings.isSosActive = false
        SOSNotificationManager.cancelEmergencySosNotification(context)
        SOSNotificationManager.showSosResolvedNotification(context)

        val dbHelper = AppDatabaseHelper(context)
        dbHelper.markLatestSosResolved()

        Toast.makeText(context, "✅ SOS Resolved. 'I'm Safe' sent.", Toast.LENGTH_SHORT).show()

        // Send 'I'm Safe' SMS to recipients
        val recipients = appSettings.sosRecipients
        val contacts = if (recipients.isNotEmpty()) recipients else dbHelper.getAllContacts().map { it.phone }
        val msg = appSettings.customSafeMessage

        for (phone in contacts) {
            if (phone.isNotBlank()) {
                sendSmsSilent(context, phone, msg)
            }
        }
        appSettings.sosRecipients = emptyList()
    }

    private fun handleCancelFall(context: Context) {
        SOSNotificationManager.cancelFallDetectionAlert(context)
        val serviceIntent = Intent(context, FallDetectionService::class.java).apply {
            action = "ACTION_CANCEL_FALL"
        }
        context.startService(serviceIntent)
        Toast.makeText(context, "✅ Fall alarm cancelled. You're safe!", Toast.LENGTH_SHORT).show()
    }

    private fun handleConfirmCheckIn(context: Context) {
        SOSNotificationManager.cancelSafetyTimerNotification(context)
        val dbHelper = AppDatabaseHelper(context)
        dbHelper.recordCheckIn("Completed", "Confirmed safe via Notification Action")
        Toast.makeText(context, "✅ Safety check-in confirmed!", Toast.LENGTH_SHORT).show()
    }

    private fun handleExtendTimer(context: Context) {
        SOSNotificationManager.showSafetyTimerNotification(context, "Extended Activity", "15 mins remaining")
        Toast.makeText(context, "⏱️ Safety Timer extended by +15 minutes.", Toast.LENGTH_SHORT).show()
    }

    private fun handleStopJourney(context: Context) {
        SOSNotificationManager.cancelJourneyTrackingNotification(context)
        Toast.makeText(context, "📍 Live commute tracking stopped.", Toast.LENGTH_SHORT).show()
    }

    private fun handleTriggerInstantSos(context: Context) {
        SOSNotificationManager.cancelFallDetectionAlert(context)
        // Also stop the fall detection confirmation timer
        val serviceIntent = Intent(context, FallDetectionService::class.java).apply {
            action = FallDetectionService.ACTION_CANCEL_FALL
        }
        context.startService(serviceIntent)
        
        val sosIntent = Intent("com.onetapsos.app.SEND_SOS").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(sosIntent)
        Toast.makeText(context, "🚨 Emergency SOS dispatched!", Toast.LENGTH_SHORT).show()
    }

    private fun handleAnswerFakeCall(context: Context) {
        SOSNotificationManager.cancelFakeCallNotification(context)
        val callIntent = Intent(context, FakeCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("AUTO_ANSWER", true)
        }
        context.startActivity(callIntent)
    }

    private fun handleDeclineFakeCall(context: Context) {
        SOSNotificationManager.cancelFakeCallNotification(context)
        Toast.makeText(context, "Call declined.", Toast.LENGTH_SHORT).show()
    }

    private fun handleSendBatterySms(context: Context) {
        val contacts = ContactsDatabaseHelper(context).getAllContacts()
        if (contacts.isEmpty()) {
            Toast.makeText(context, "No trusted contacts configured.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                sendBatterySms(context, contacts.map { it.phone }, loc)
            }.addOnFailureListener {
                sendBatterySms(context, contacts.map { it.phone }, null)
            }
        } catch (e: SecurityException) {
            sendBatterySms(context, contacts.map { it.phone }, null)
        }
    }

    private fun sendBatterySms(context: Context, phones: List<String>, location: Location?) {
        val mapsLink = if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else "Location unavailable"
        val timeStamp = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
        val message = "🔋 CRITICAL BATTERY ALERT (5%)\n\nMy device is shutting down soon.\n📍 Last Known Location: $mapsLink\n🕒 Time: $timeStamp\n\nOneTapSOS Emergency Alert."

        var count = 0
        for (phone in phones) {
            if (phone.isNotBlank()) {
                if (sendSmsSilent(context, phone, message)) count++
            }
        }
        Toast.makeText(context, "✅ Battery emergency alert sent to $count contact(s)!", Toast.LENGTH_LONG).show()
    }

    private fun sendSmsSilent(context: Context, phoneNumber: String, message: String): Boolean {
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
            false
        }
    }
}
