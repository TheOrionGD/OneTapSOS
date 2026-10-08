package com.onetapsos.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object SOSNotificationManager {

    // --- Channel IDs ---
    const val CHANNEL_ID_EMERGENCY = "sosense_emergency_sos"
    const val CHANNEL_ID_FALL = "sosense_fall_detection"
    const val CHANNEL_ID_SAFETY_TIMER = "sosense_safety_timer"
    const val CHANNEL_ID_JOURNEY = "sosense_journey_tracking"
    const val CHANNEL_ID_BATTERY = "sosense_battery_alert"
    const val CHANNEL_ID_SAFEZONE = "sosense_safe_zone"
    const val CHANNEL_ID_FAKE_CALL = "sosense_fake_call"
    const val CHANNEL_ID_TIPS = "sosense_safety_tips"

    // --- Notification IDs ---
    const val NOTIF_ID_EMERGENCY = 1001
    const val NOTIF_ID_FALL = 2001
    const val NOTIF_ID_SAFETY_TIMER = 3001
    const val NOTIF_ID_JOURNEY = 4001
    const val NOTIF_ID_BATTERY = 5001
    const val NOTIF_ID_SAFEZONE = 6001
    const val NOTIF_ID_FAKE_CALL = 7001
    const val NOTIF_ID_TIPS = 8001
    const val NOTIF_ID_BROADCAST = 9001
    const val NOTIF_ID_RESOLVED = 1002

    /**
     * Initializes all notification channels with tailored importance, sounds, and vibration.
     */
    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributesAlarm = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val audioAttributesNotif = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Emergency SOS Channel
            val emergencyChannel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "🚨 Emergency SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority emergency SOS activation, broadcasting and dispatch notices"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmSound, audioAttributesAlarm)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Fall Detection Channel
            val fallChannel = NotificationChannel(
                CHANNEL_ID_FALL,
                "🏃 Fall Detection Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent warnings and countdown when high-impact fall is detected"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200)
                setSound(alarmSound, audioAttributesAlarm)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Safety Timer & Check-In Channel
            val timerChannel = NotificationChannel(
                CHANNEL_ID_SAFETY_TIMER,
                "⏱️ Safety Timer & Check-Ins",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Safety countdown reminders and overdue check-in alerts"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(notifSound, audioAttributesNotif)
            }

            // 4. Journey & Live Location Sharing Channel
            val journeyChannel = NotificationChannel(
                CHANNEL_ID_JOURNEY,
                "📍 Journey & Live Location",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent status when live commute tracking or route monitoring is active"
                setShowBadge(false)
            }

            // 5. Critical Battery Alert Channel
            val batteryChannel = NotificationChannel(
                CHANNEL_ID_BATTERY,
                "🔋 Critical Battery Alerts (5%)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency notifications when battery reaches critical 5% depletion"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500)
                setSound(notifSound, audioAttributesNotif)
            }

            // 6. Safe Zone & Geofencing Channel
            val safeZoneChannel = NotificationChannel(
                CHANNEL_ID_SAFEZONE,
                "🛡️ Safe Zones & Geofences",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when entering or leaving configured safe geographic zones"
                enableLights(true)
                lightColor = Color.GREEN
            }

            // 7. Fake Call Simulator Channel
            val fakeCallChannel = NotificationChannel(
                CHANNEL_ID_FAKE_CALL,
                "📞 Fake Call Incoming Simulation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Simulated incoming telephone call heads-up notification"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), audioAttributesAlarm)
            }

            // 8. Daily Safety Tips Channel
            val tipsChannel = NotificationChannel(
                CHANNEL_ID_TIPS,
                "💡 Safety Tips & Readiness",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily preparedness and personal security tips"
            }

            notificationManager.createNotificationChannels(
                listOf(
                    emergencyChannel,
                    fallChannel,
                    timerChannel,
                    journeyChannel,
                    batteryChannel,
                    safeZoneChannel,
                    fakeCallChannel,
                    tipsChannel
                )
            )
        }
    }

    // ==========================================
    // 1. EMERGENCY SOS NOTIFICATION
    // ==========================================
    fun showEmergencySosNotification(
        context: Context,
        locationSummary: String = "Live GPS Broadcast Active",
        contactsCount: Int = 1
    ) {
        val appSettings = AppSettings(context)
        if (!appSettings.isNotifSosEnabled) return

        // PendingIntent for clicking notification body -> SOSActiveActivity
        val openIntent = Intent(context, SOSActiveActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 101, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "I'm Safe" (Resolve)
        val resolveIntent = Intent(context, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_RESOLVE_SOS
        }
        val resolvePendingIntent = PendingIntent.getBroadcast(
            context, 102, resolveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "View Live Map"
        val mapIntent = Intent(context, MapActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val mapPendingIntent = PendingIntent.getActivity(
            context, 103, mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 EMERGENCY SOS BROADCAST ACTIVE")
            .setContentText("Dispatched to $contactsCount contact(s) • $locationSummary")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🚨 EMERGENCY SOS IS ACTIVE\n\nYour emergency distress signal and live GPS breadcrumbs have been broadcast to $contactsCount trusted contact(s).\n\nStatus: $locationSummary")
            )
            .setColor(0xFFE63946.toInt())
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "✅ I'm Safe (Resolve)", resolvePendingIntent)
            .addAction(android.R.drawable.ic_dialog_map, "🗺️ View Map", mapPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_EMERGENCY, notification)
    }

    fun cancelEmergencySosNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID_EMERGENCY)
    }

    // ==========================================
    // 2. SOS RESOLVED CONFIRMATION
    // ==========================================
    fun showSosResolvedNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✅ Emergency Resolved — You're Safe")
            .setContentText("I'm Safe confirmation message was sent to your trusted contacts.")
            .setColor(0xFF06D6A0.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_RESOLVED, notification)
    }

    // ==========================================
    // 3. FALL DETECTION WARNING NOTIFICATION
    // ==========================================
    fun showFallDetectionAlertNotification(context: Context, secondsRemaining: Int = 15) {
        val appSettings = AppSettings(context)
        if (!appSettings.isNotifFallEnabled) return

        // Action: Cancel Fall Alert (I'm OK)
        val cancelIntent = Intent(context, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_CANCEL_FALL
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, 201, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Trigger Instant SOS Now
        val triggerIntent = Intent(context, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_TRIGGER_INSTANT_SOS
        }
        val triggerPendingIntent = PendingIntent.getBroadcast(
            context, 202, triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FALL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Fall Detected! Are you OK?")
            .setContentText("Automatic SOS dispatch in $secondsRemaining seconds...")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("⚠️ High impact fall detected by motion sensors.\n\nSending emergency distress SMS with your coordinates in $secondsRemaining seconds unless cancelled.")
            )
            .setColor(0xFFFFD60A.toInt())
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "✅ I'm OK (Cancel)", cancelPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "🚨 Send SOS Now", triggerPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_FALL, notification)
    }

    fun cancelFallDetectionAlert(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID_FALL)
    }

    // ==========================================
    // 4. SAFETY TIMER NOTIFICATION
    // ==========================================
    fun showSafetyTimerNotification(
        context: Context,
        reason: String,
        timeRemainingText: String
    ) {
        val appSettings = AppSettings(context)
        if (!appSettings.isNotifCheckInEnabled) return

        val openIntent = Intent(context, SafetyTimerActiveActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 301, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val checkInIntent = Intent(context, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_CONFIRM_CHECK_IN
        }
        val checkInPendingIntent = PendingIntent.getBroadcast(
            context, 302, checkInIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val extendIntent = Intent(context, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_EXTEND_TIMER
        }
        val extendPendingIntent = PendingIntent.getBroadcast(
            context, 303, extendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SAFETY_TIMER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏱️ Safety Timer Active: $reason")
            .setContentText("Remaining: $timeRemainingText • Tap to check in")
            .setContentIntent(openPendingIntent)
            .setColor(0xFF00D9FF.toInt())
            .addAction(android.R.drawable.checkbox_on_background, "✅ Check-In (I'm Safe)", checkInPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "⏱️ +15 Mins", extendPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_SAFETY_TIMER, notification)
    }

    fun cancelSafetyTimerNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID_SAFETY_TIMER)
    }

    // ==========================================
    // 5. MISSED CHECK-IN / OVERDUE ALERT
    // ==========================================
    fun showMissedCheckInNotification(context: Context, reason: String = "Scheduled Check-In") {
        val openIntent = Intent(context, SOSActivationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 304, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SAFETY_TIMER)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Missed Safety Check-In!")
            .setContentText("Your safety window for '$reason' expired.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("⚠️ You did not check in on time for '$reason'.\n\nTap to send an instant SOS or confirm your safety with trusted contacts.")
            )
            .setContentIntent(openPendingIntent)
            .setColor(0xFFEF233C.toInt())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_SAFETY_TIMER, notification)
    }

    // ==========================================
    // 6. JOURNEY / LIVE LOCATION TRACKING
    // ==========================================
    fun showJourneyTrackingNotification(
        context: Context,
        destination: String = "Active Route",
        statusText: String = "Sharing real-time location trail"
    ) {
        val appSettings = AppSettings(context)
        if (!appSettings.isNotifJourneyEnabled) return

        val openIntent = Intent(context, LiveTrackingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 401, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_STOP_JOURNEY
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 402, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_JOURNEY)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("📍 Journey Monitor: $destination")
            .setContentText(statusText)
            .setContentIntent(openPendingIntent)
            .setColor(0xFF0077B6.toInt())
            .addAction(android.R.drawable.ic_delete, "⏹ Stop Sharing", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_JOURNEY, notification)
    }

    fun cancelJourneyTrackingNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID_JOURNEY)
    }

    // ==========================================
    // 7. CRITICAL 5% BATTERY ALERT
    // ==========================================
    fun showLowBatteryAlertNotification(context: Context, batteryPct: Int = 5) {
        val appSettings = AppSettings(context)
        if (!appSettings.isNotifBatteryEnabled) return

        val sendSmsIntent = Intent(context, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_SEND_BATTERY_SMS
        }
        val sendSmsPendingIntent = PendingIntent.getBroadcast(
            context, 501, sendSmsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(context, BatterySafetyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 502, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BATTERY)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentTitle("🔋 Critical Battery Alert ($batteryPct%)")
            .setContentText("Emergency battery safety mode engaged.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🔋 Battery is at $batteryPct%!\n\nOneTapSOS can send an automated low-power emergency SMS with your last known GPS coordinates to trusted contacts before power loss.")
            )
            .setColor(0xFFEF233C.toInt())
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "📲 Send Battery SOS SMS", sendSmsPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_BATTERY, notification)
    }

    // ==========================================
    // 8. SAFE ZONE / GEOFENCE ALERT
    // ==========================================
    fun showSafeZoneAlertNotification(
        context: Context,
        zoneName: String = "Home Safe Zone",
        isLeaving: Boolean = true
    ) {
        val appSettings = AppSettings(context)
        if (!appSettings.isNotifSafeZoneEnabled) return

        val actionText = if (isLeaving) "Departed Safe Zone" else "Arrived at Safe Zone"
        val descText = if (isLeaving)
            "You have exited '$zoneName'. Safety tracking recommended."
        else
            "You have entered '$zoneName'. You are in a designated safe area."

        val openIntent = Intent(context, SafeMapActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 601, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SAFEZONE)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("🛡️ Geofence: $actionText")
            .setContentText(descText)
            .setContentIntent(openPendingIntent)
            .setColor(if (isLeaving) 0xFFFFD60A.toInt() else 0xFF06D6A0.toInt())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_SAFEZONE, notification)
    }

    // ==========================================
    // 9. FAKE CALL INCOMING SIMULATION
    // ==========================================
    fun showFakeCallIncomingNotification(
        context: Context,
        callerName: String = "Police Dispatch / Security"
    ) {
        val answerIntent = Intent(context, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_ANSWER_FAKE_CALL
        }
        val answerPendingIntent = PendingIntent.getBroadcast(
            context, 701, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_DECLINE_FAKE_CALL
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context, 702, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(context, FakeCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 703, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FAKE_CALL)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("📞 Incoming Call")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setColor(0xFF06D6A0.toInt())
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_call, "📞 Answer", answerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "❌ Decline", declinePendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_FAKE_CALL, notification)
    }

    fun cancelFakeCallNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID_FAKE_CALL)
    }

    // ==========================================
    // 10. DAILY SAFETY TIP / BROADCAST NOTIFICATION
    // ==========================================
    fun showSafetyTipNotification(
        context: Context,
        tipTitle: String = "Personal Safety Protocol",
        tipMessage: String = "Always share your commute route with a trusted contact before travelling alone at night."
    ) {
        val appSettings = AppSettings(context)
        if (!appSettings.isNotifTipsEnabled) return

        val openIntent = Intent(context, SafetyTipsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 801, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TIPS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💡 $tipTitle")
            .setContentText(tipMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText("💡 $tipTitle\n\n$tipMessage"))
            .setContentIntent(openPendingIntent)
            .setColor(0xFF00D9FF.toInt())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_TIPS, notification)
    }

    fun showBroadcastSentNotification(context: Context, count: Int = 1) {
        val openIntent = Intent(context, MessageHistoryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 901, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("📡 Emergency Broadcast Dispatched")
            .setContentText("Distress message successfully transmitted to $count contact(s).")
            .setContentIntent(openPendingIntent)
            .setColor(0xFF00D9FF.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_BROADCAST, notification)
    }
}
