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
        const val DEFAULT_SAFE_MESSAGE =
            "I'm Safe\n\nThe situation has been resolved. I am safe now.\n\nThank you for your concern."
    }

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
