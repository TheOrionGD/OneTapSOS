package com.sosence.app

import android.content.Context
import android.content.SharedPreferences

class AppSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "sosence_settings"
        private const val KEY_BATTERY_ALERT_ENABLED = "battery_alert_enabled"
        private const val KEY_SOS_TIMER_SECONDS = "sos_timer_seconds"
        private const val KEY_CUSTOM_SAFE_MESSAGE = "custom_safe_message"
        private const val KEY_IS_SOS_ACTIVE = "is_sos_active"
        private const val KEY_LAST_SOS_TIMESTAMP = "last_sos_timestamp"
        private const val KEY_SOS_RECIPIENTS = "sos_recipients"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_NOTIF_SOS = "notif_sos"
        private const val KEY_NOTIF_FALL = "notif_fall"
        private const val KEY_NOTIF_CHECKIN = "notif_checkin"
        private const val KEY_NOTIF_BATTERY = "notif_battery"
        private const val KEY_NOTIF_JOURNEY = "notif_journey"
        private const val KEY_NOTIF_SAFEZONE = "notif_safezone"
        private const val KEY_NOTIF_TIPS = "notif_tips"

        // Background Safety Engine Keys
        private const val KEY_BG_SAFETY_ENABLED = "bg_safety_enabled"
        private const val KEY_LOW_BATTERY_THRESHOLD = "low_battery_threshold"
        private const val KEY_BATTERY_VIBRATION_ENABLED = "battery_vibration_enabled"
        private const val KEY_SAFETY_TIMER_ESCALATION = "safety_timer_escalation"
        private const val KEY_JOURNEY_MONITORING = "journey_monitoring_enabled"
        private const val KEY_LOW_BATTERY_STATE_ACTIVE = "low_battery_state_active"
        private const val KEY_LAST_LOW_BATTERY_TRIGGER_TS = "last_low_battery_trigger_ts"
        private const val KEY_LAST_KNOWN_BATTERY_LEVEL = "last_known_battery_level"
        private const val KEY_ACTIVE_SAFETY_TIMER_END_TIME = "active_safety_timer_end_time"
        private const val KEY_ACTIVE_SAFETY_TIMER_REASON = "active_safety_timer_reason"
        private const val KEY_ACTIVE_SAFETY_TIMER_MINUTES = "active_safety_timer_minutes"
        private const val KEY_ACTIVE_CHECKIN_END_TIME = "active_checkin_end_time"
        private const val KEY_ACTIVE_CHECKIN_NOTE = "active_checkin_note"

        // Fall Detection Keys
        private const val KEY_FALL_DETECTION_ENABLED = "fall_detection_enabled"
        private const val KEY_FALL_IMPACT_THRESHOLD = "fall_impact_threshold"
        private const val KEY_FALL_FREEFALL_THRESHOLD = "fall_freefall_threshold"
        private const val KEY_FALL_CONFIRMATION_TIMEOUT = "fall_confirmation_timeout"

        const val DEFAULT_SAFE_MESSAGE =
            "I'm Safe\n\nThe situation has been resolved. I am safe now.\n\nThank you for your concern."
    }

    // Fall Detection Properties
    var isFallDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_FALL_DETECTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FALL_DETECTION_ENABLED, value).apply()

    var fallImpactThreshold: Float
        get() = prefs.getFloat(KEY_FALL_IMPACT_THRESHOLD, 22.0f)
        set(value) = prefs.edit().putFloat(KEY_FALL_IMPACT_THRESHOLD, value).apply()

    var fallFreeFallThreshold: Float
        get() = prefs.getFloat(KEY_FALL_FREEFALL_THRESHOLD, 3.5f)
        set(value) = prefs.edit().putFloat(KEY_FALL_FREEFALL_THRESHOLD, value).apply()

    var fallConfirmationTimeoutSeconds: Int
        get() = prefs.getInt(KEY_FALL_CONFIRMATION_TIMEOUT, 15)
        set(value) = prefs.edit().putInt(KEY_FALL_CONFIRMATION_TIMEOUT, value).apply()

    // Background Safety Engine Properties
    var isBackgroundSafetyEnabled: Boolean
        get() = prefs.getBoolean(KEY_BG_SAFETY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BG_SAFETY_ENABLED, value).apply()

    var lowBatteryThreshold: Int
        get() = prefs.getInt(KEY_LOW_BATTERY_THRESHOLD, 5)
        set(value) = prefs.edit().putInt(KEY_LOW_BATTERY_THRESHOLD, value).apply()

    var isBatteryVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_VIBRATION_ENABLED, value).apply()

    var isSafetyTimerEscalationEnabled: Boolean
        get() = prefs.getBoolean(KEY_SAFETY_TIMER_ESCALATION, true)
        set(value) = prefs.edit().putBoolean(KEY_SAFETY_TIMER_ESCALATION, value).apply()

    var isJourneyMonitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_JOURNEY_MONITORING, true)
        set(value) = prefs.edit().putBoolean(KEY_JOURNEY_MONITORING, value).apply()

    var isLowBatteryStateActive: Boolean
        get() = prefs.getBoolean(KEY_LOW_BATTERY_STATE_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_LOW_BATTERY_STATE_ACTIVE, value).apply()

    var lastLowBatteryTriggerTimestamp: Long
        get() = prefs.getLong(KEY_LAST_LOW_BATTERY_TRIGGER_TS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_LOW_BATTERY_TRIGGER_TS, value).apply()

    var lastKnownBatteryLevel: Int
        get() = prefs.getInt(KEY_LAST_KNOWN_BATTERY_LEVEL, 100)
        set(value) = prefs.edit().putInt(KEY_LAST_KNOWN_BATTERY_LEVEL, value).apply()

    var activeSafetyTimerEndTime: Long
        get() = prefs.getLong(KEY_ACTIVE_SAFETY_TIMER_END_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_SAFETY_TIMER_END_TIME, value).apply()

    var activeSafetyTimerReason: String
        get() = prefs.getString(KEY_ACTIVE_SAFETY_TIMER_REASON, "Safety Monitoring") ?: "Safety Monitoring"
        set(value) = prefs.edit().putString(KEY_ACTIVE_SAFETY_TIMER_REASON, value).apply()

    var activeSafetyTimerMinutes: Int
        get() = prefs.getInt(KEY_ACTIVE_SAFETY_TIMER_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_ACTIVE_SAFETY_TIMER_MINUTES, value).apply()

    var activeCheckInEndTime: Long
        get() = prefs.getLong(KEY_ACTIVE_CHECKIN_END_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_CHECKIN_END_TIME, value).apply()

    var activeCheckInNote: String
        get() = prefs.getString(KEY_ACTIVE_CHECKIN_NOTE, "Scheduled Check-In") ?: "Scheduled Check-In"
        set(value) = prefs.edit().putString(KEY_ACTIVE_CHECKIN_NOTE, value).apply()

    var isNotifSosEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SOS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SOS, value).apply()

    var isNotifFallEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_FALL, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_FALL, value).apply()

    var isNotifCheckInEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_CHECKIN, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_CHECKIN, value).apply()

    var isNotifBatteryEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_BATTERY, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_BATTERY, value).apply()

    var isNotifJourneyEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_JOURNEY, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_JOURNEY, value).apply()

    var isNotifSafeZoneEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SAFEZONE, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SAFEZONE, value).apply()

    var isNotifTipsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_TIPS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_TIPS, value).apply()

    var isBatteryAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_ALERT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_ALERT_ENABLED, value).apply()

    var sosTimerSeconds: Int
        get() = prefs.getInt(KEY_SOS_TIMER_SECONDS, 5)
        set(value) = prefs.edit().putInt(KEY_SOS_TIMER_SECONDS, value).apply()

    var customSafeMessage: String
        get() = prefs.getString(KEY_CUSTOM_SAFE_MESSAGE, DEFAULT_SAFE_MESSAGE) ?: DEFAULT_SAFE_MESSAGE
        set(value) = prefs.edit().putString(KEY_CUSTOM_SAFE_MESSAGE, value).apply()

    var isSosActive: Boolean
        get() = prefs.getBoolean(KEY_IS_SOS_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SOS_ACTIVE, value).apply()

    var lastSosTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SOS_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SOS_TIMESTAMP, value).apply()

    var sosRecipients: List<String>
        get() {
            val raw = prefs.getString(KEY_SOS_RECIPIENTS, "") ?: ""
            return if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        set(phones) {
            prefs.edit().putString(KEY_SOS_RECIPIENTS, phones.joinToString(",")).apply()
        }

    var languageCode: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun applyLocale(context: Context): Context {
        val locale = java.util.Locale(languageCode)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
