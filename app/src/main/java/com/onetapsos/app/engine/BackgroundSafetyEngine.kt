package com.onetapsos.app.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.onetapsos.app.AppSettings
import com.onetapsos.app.SOSNotificationManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object BackgroundSafetyEngine {

    private const val TAG = "BackgroundSafetyEngine"
    private var isInitialized = false

    private val _eventFlow = MutableSharedFlow<SafetyEvent>(extraBufferCapacity = 64)
    val eventFlow: SharedFlow<SafetyEvent> = _eventFlow.asSharedFlow()

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        Log.i(TAG, "Initializing BackgroundSafetyEngine...")

        // 1. Initial sticky battery check
        checkCurrentBatteryState(appContext)

        // 2. Reschedule any existing timers that survived
        restorePendingSchedules(appContext)
    }

    fun dispatchEvent(context: Context, event: SafetyEvent) {
        val appContext = context.applicationContext
        try {
            SafetyEventProcessor(appContext).processEvent(event)
            _eventFlow.tryEmit(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing event ${event.javaClass.simpleName}: ${e.message}", e)
        }
    }

    fun checkCurrentBatteryState(context: Context) {
        try {
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, batteryFilter) ?: return

            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val healthCode = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            val tempTenths = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

            val pct = if (level >= 0 && scale > 0) ((level * 100) / scale) else -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val tempCelsius = tempTenths / 10.0f

            val health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                else -> "Normal"
            }

            if (pct >= 0) {
                val appSettings = AppSettings(context)
                appSettings.lastKnownBatteryLevel = pct

                if (pct <= appSettings.lowBatteryThreshold && !isCharging) {
                    dispatchEvent(context, SafetyEvent.LowBattery(pct, isCharging, tempCelsius, health))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read battery state: ${e.message}")
        }
    }

    fun scheduleSafetyTimer(context: Context, minutes: Int, reason: String) {
        val appContext = context.applicationContext
        val appSettings = AppSettings(appContext)
        val triggerTimeMs = System.currentTimeMillis() + (minutes * 60 * 1000L)

        appSettings.activeSafetyTimerEndTime = triggerTimeMs
        appSettings.activeSafetyTimerReason = reason
        appSettings.activeSafetyTimerMinutes = minutes

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(appContext, SafetyAlarmReceiver::class.java).apply {
            action = SafetyAlarmReceiver.ACTION_SAFETY_TIMER_EXPIRED
            putExtra(SafetyAlarmReceiver.EXTRA_REASON, reason)
            putExtra(SafetyAlarmReceiver.EXTRA_MINUTES, minutes)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            SafetyAlarmReceiver.REQ_CODE_SAFETY_TIMER,
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
            Log.i(TAG, "Exact Safety Timer scheduled for $minutes mins (Trigger: $triggerTimeMs)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing. Falling back to standard alarm: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }

        SOSNotificationManager.showSafetyTimerNotification(appContext, reason, "$minutes mins remaining")
    }

    fun cancelSafetyTimer(context: Context) {
        val appContext = context.applicationContext
        val appSettings = AppSettings(appContext)
        appSettings.activeSafetyTimerEndTime = 0L

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(appContext, SafetyAlarmReceiver::class.java).apply {
            action = SafetyAlarmReceiver.ACTION_SAFETY_TIMER_EXPIRED
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            SafetyAlarmReceiver.REQ_CODE_SAFETY_TIMER,
            intent,
            flags
        )
        alarmManager?.cancel(pendingIntent)
        SOSNotificationManager.cancelSafetyTimerNotification(appContext)
        Log.i(TAG, "Safety Timer cancelled")
    }

    fun scheduleCheckIn(context: Context, minutes: Int, note: String) {
        val appContext = context.applicationContext
        val appSettings = AppSettings(appContext)
        val triggerTimeMs = System.currentTimeMillis() + (minutes * 60 * 1000L)

        appSettings.activeCheckInEndTime = triggerTimeMs
        appSettings.activeCheckInNote = note

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(appContext, SafetyAlarmReceiver::class.java).apply {
            action = SafetyAlarmReceiver.ACTION_CHECKIN_EXPIRED
            putExtra(SafetyAlarmReceiver.EXTRA_NOTE, note)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            SafetyAlarmReceiver.REQ_CODE_CHECKIN,
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
            Log.i(TAG, "Exact Check-In scheduled for $minutes mins (Trigger: $triggerTimeMs)")
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }

    fun cancelCheckIn(context: Context) {
        val appContext = context.applicationContext
        val appSettings = AppSettings(appContext)
        appSettings.activeCheckInEndTime = 0L

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(appContext, SafetyAlarmReceiver::class.java).apply {
            action = SafetyAlarmReceiver.ACTION_CHECKIN_EXPIRED
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            SafetyAlarmReceiver.REQ_CODE_CHECKIN,
            intent,
            flags
        )
        alarmManager?.cancel(pendingIntent)
    }

    fun restorePendingSchedules(context: Context) {
        val appSettings = AppSettings(context)
        val now = System.currentTimeMillis()

        // Check Safety Timer
        val timerEnd = appSettings.activeSafetyTimerEndTime
        if (timerEnd > 0L) {
            if (timerEnd > now) {
                val remainingMins = ((timerEnd - now) / 60000).toInt().coerceAtLeast(1)
                scheduleSafetyTimer(context, remainingMins, appSettings.activeSafetyTimerReason)
                Log.i(TAG, "Restored active safety timer: $remainingMins mins remaining")
            } else {
                // Expired while offline/rebooting
                dispatchEvent(context, SafetyEvent.SafetyTimerExpired(appSettings.activeSafetyTimerReason, appSettings.activeSafetyTimerMinutes))
            }
        }

        // Check Check-In
        val checkInEnd = appSettings.activeCheckInEndTime
        if (checkInEnd > 0L) {
            if (checkInEnd > now) {
                val remainingMins = ((checkInEnd - now) / 60000).toInt().coerceAtLeast(1)
                scheduleCheckIn(context, remainingMins, appSettings.activeCheckInNote)
                Log.i(TAG, "Restored active check-in: $remainingMins mins remaining")
            } else {
                dispatchEvent(context, SafetyEvent.CheckInExpired(appSettings.activeCheckInNote))
            }
        }
    }
}
