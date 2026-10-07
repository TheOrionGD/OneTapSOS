package com.sosence.app.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.sosence.app.AppSettings
import com.sosence.app.SOSNotificationManager
import com.sosence.app.data.AppDatabaseHelper
import com.sosence.app.data.SafetyEventRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafetyEventProcessor(private val context: Context) {

    private val appSettings by lazy { AppSettings(context) }
    private val dbHelper by lazy { AppDatabaseHelper(context) }

    companion object {
        private const val TAG = "SafetyEventProcessor"
        private const val BATTERY_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes deduplication cooldown
    }

    fun processEvent(event: SafetyEvent) {
        if (!appSettings.isBackgroundSafetyEnabled) {
            Log.d(TAG, "Background safety engine is disabled by user settings. Skipping event: ${event.javaClass.simpleName}")
            return
        }

        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        val formattedTime = sdf.format(Date(event.timestamp))

        when (event) {
            is SafetyEvent.LowBattery -> handleLowBattery(event, formattedTime)
            is SafetyEvent.CriticalBattery -> handleCriticalBattery(event, formattedTime)
            is SafetyEvent.ChargingStarted -> handleChargingState(event, true, formattedTime)
            is SafetyEvent.ChargingStopped -> handleChargingState(event, false, formattedTime)
            is SafetyEvent.BatteryRecovered -> handleBatteryRecovered(event, formattedTime)
            is SafetyEvent.SafetyTimerExpired -> handleSafetyTimerExpired(event, formattedTime)
            is SafetyEvent.SafetyTimerReminder -> handleSafetyTimerReminder(event, formattedTime)
            is SafetyEvent.CheckInExpired -> handleCheckInExpired(event, formattedTime)
            is SafetyEvent.JourneyDeviation -> handleJourneyDeviation(event, formattedTime)
            is SafetyEvent.GeofenceEntered -> handleGeofenceEvent(event, true, formattedTime)
            is SafetyEvent.GeofenceExited -> handleGeofenceEvent(event, false, formattedTime)
            is SafetyEvent.BootRestored -> handleBootRestored(event, formattedTime)
            is SafetyEvent.ConnectivityChanged -> handleConnectivity(event, formattedTime)
            is SafetyEvent.FallDetected -> handleFall(event, formattedTime)
            is SafetyEvent.ManualSosTriggered -> handleManualSos(event, formattedTime)
        }
    }

    private fun handleLowBattery(event: SafetyEvent.LowBattery, formattedTime: String) {
        appSettings.lastKnownBatteryLevel = event.percentage

        // If charging, do not trigger alert
        if (event.isCharging) {
            Log.d(TAG, "Battery level is ${event.percentage}%, but device is currently charging. Skipping alert.")
            return
        }

        val threshold = appSettings.lowBatteryThreshold
        if (event.percentage > threshold) {
            // Check if battery recovered above threshold + 5 (e.g., 10% or 15%) to reset state
            if (event.percentage >= threshold + 5 && appSettings.isLowBatteryStateActive) {
                appSettings.isLowBatteryStateActive = false
                Log.d(TAG, "Battery recovered to ${event.percentage}%. Reset low battery state latch.")
            }
            return
        }

        if (!appSettings.isBatteryAlertEnabled) {
            Log.d(TAG, "Battery alert setting is disabled by user.")
            return
        }

        // Deduplication & Cooldown Check
        val now = System.currentTimeMillis()
        val lastTrigger = appSettings.lastLowBatteryTriggerTimestamp
        val isCooldownPassed = (now - lastTrigger) > BATTERY_COOLDOWN_MS

        if (appSettings.isLowBatteryStateActive && !isCooldownPassed) {
            Log.d(TAG, "Low battery event suppressed: active state latch is TRUE and cooldown has not expired.")
            return
        }

        // Trigger Event
        appSettings.isLowBatteryStateActive = true
        appSettings.lastLowBatteryTriggerTimestamp = now

        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "LOW_BATTERY",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                batteryLevel = event.percentage,
                details = "Battery depleted to ${event.percentage}% (Threshold: $threshold%). Health: ${event.health}"
            )
        )

        // Show High-Priority Notification
        SOSNotificationManager.showLowBatteryAlertNotification(context, event.percentage)

        // Vibrate if enabled
        if (appSettings.isBatteryVibrationEnabled) {
            triggerHapticAlert()
        }

        Log.i(TAG, "⚡ Low Battery Safety Event Triggered: ${event.percentage}% (Threshold: $threshold%)")
    }

    private fun handleCriticalBattery(event: SafetyEvent.CriticalBattery, formattedTime: String) {
        appSettings.lastKnownBatteryLevel = event.percentage
        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "CRITICAL_BATTERY",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                batteryLevel = event.percentage,
                details = "Critical battery state at ${event.percentage}%"
            )
        )
        SOSNotificationManager.showLowBatteryAlertNotification(context, event.percentage)
    }

    private fun handleChargingState(event: SafetyEvent, isCharging: Boolean, formattedTime: String) {
        val pct = when (event) {
            is SafetyEvent.ChargingStarted -> event.percentage
            is SafetyEvent.ChargingStopped -> event.percentage
            else -> appSettings.lastKnownBatteryLevel
        }
        appSettings.lastKnownBatteryLevel = pct

        if (isCharging) {
            appSettings.isLowBatteryStateActive = false
        }

        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = if (isCharging) "CHARGING_STARTED" else "CHARGING_STOPPED",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                batteryLevel = pct,
                details = if (isCharging) "Connected to power source at $pct%" else "Disconnected from power source at $pct%"
            )
        )
    }

    private fun handleBatteryRecovered(event: SafetyEvent.BatteryRecovered, formattedTime: String) {
        appSettings.isLowBatteryStateActive = false
        appSettings.lastKnownBatteryLevel = event.percentage
        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "BATTERY_RECOVERED",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                batteryLevel = event.percentage,
                details = "Battery recovered to ${event.percentage}%"
            )
        )
    }

    private fun handleSafetyTimerExpired(event: SafetyEvent.SafetyTimerExpired, formattedTime: String) {
        // Clear active timer in settings
        appSettings.activeSafetyTimerEndTime = 0L

        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "SAFETY_TIMER_EXPIRED",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                details = "Safety timer (${event.totalMinutes}m) expired without user confirmation: ${event.reason}"
            )
        )

        dbHelper.recordCheckIn("Missed", "Safety timer expired: ${event.reason}")

        // Cancel countdown notification and post Missed Check-In Alert
        SOSNotificationManager.cancelSafetyTimerNotification(context)
        SOSNotificationManager.showMissedCheckInNotification(context, event.reason)

        // Escalation to Emergency SOS workflow if enabled
        if (appSettings.isSafetyTimerEscalationEnabled) {
            Log.w(TAG, "Safety timer escalation policy active. Escalating to SOS dispatch...")
            val sosIntent = Intent("com.sosence.app.SEND_SOS").apply {
                setPackage(context.packageName)
                putExtra("TRIGGER_SOURCE", "SAFETY_TIMER_TIMEOUT")
                putExtra("REASON", event.reason)
            }
            context.sendBroadcast(sosIntent)
        }
    }

    private fun handleSafetyTimerReminder(event: SafetyEvent.SafetyTimerReminder, formattedTime: String) {
        SOSNotificationManager.showSafetyTimerNotification(
            context,
            event.reason,
            "${event.remainingMinutes} mins remaining"
        )
    }

    private fun handleCheckInExpired(event: SafetyEvent.CheckInExpired, formattedTime: String) {
        appSettings.activeCheckInEndTime = 0L

        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "CHECKIN_EXPIRED",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                details = "Scheduled check-in expired: ${event.note}"
            )
        )

        dbHelper.recordCheckIn("Missed", "Scheduled check-in missed: ${event.note}")
        SOSNotificationManager.showMissedCheckInNotification(context, event.note)
    }

    private fun handleJourneyDeviation(event: SafetyEvent.JourneyDeviation, formattedTime: String) {
        if (!appSettings.isJourneyMonitoringEnabled) return

        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "JOURNEY_DEVIATION",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                latitude = event.latitude,
                longitude = event.longitude,
                details = "Route deviation detected (${event.deviationDistanceMeters.toInt()}m from planned path)"
            )
        )

        SOSNotificationManager.showJourneyTrackingNotification(
            context,
            "Route Deviation Alert",
            "You are ${event.deviationDistanceMeters.toInt()}m away from your intended route."
        )
    }

    private fun handleGeofenceEvent(event: SafetyEvent, isEnter: Boolean, formattedTime: String) {
        val zoneName = when (event) {
            is SafetyEvent.GeofenceEntered -> event.zoneName
            is SafetyEvent.GeofenceExited -> event.zoneName
            else -> "Safe Zone"
        }

        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = if (isEnter) "GEOFENCE_ENTERED" else "GEOFENCE_EXITED",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                details = if (isEnter) "Entered safe zone: $zoneName" else "Departed from safe zone: $zoneName"
            )
        )

        SOSNotificationManager.showSafeZoneAlertNotification(
            context,
            zoneName,
            isLeaving = !isEnter
        )
    }

    private fun handleBootRestored(event: SafetyEvent.BootRestored, formattedTime: String) {
        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "BOOT_RESTORED",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                details = "Device restarted. Restored ${event.restoredTimersCount} active safety schedules."
            )
        )
        Log.i(TAG, "Device boot recovery completed. Restored ${event.restoredTimersCount} timers.")
    }

    private fun handleConnectivity(event: SafetyEvent.ConnectivityChanged, formattedTime: String) {
        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "CONNECTIVITY_CHANGED",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                details = if (event.isConnected) "Network connected: ${event.networkType}" else "Network offline"
            )
        )
    }

    private fun handleFall(event: SafetyEvent.FallDetected, formattedTime: String) {
        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "FALL_DETECTED",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                details = "High-impact fall detected (Confidence: ${(event.confidence * 100).toInt()}%)"
            )
        )
        SOSNotificationManager.showFallDetectionAlertNotification(context, 15)
    }

    private fun handleManualSos(event: SafetyEvent.ManualSosTriggered, formattedTime: String) {
        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "MANUAL_SOS_TRIGGERED",
                timestamp = event.timestamp,
                formattedTime = formattedTime,
                priority = event.priority.name,
                details = "Emergency SOS activated via ${event.triggerSource}"
            )
        )
    }

    private fun triggerHapticAlert() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate: ${e.message}")
        }
    }
}
