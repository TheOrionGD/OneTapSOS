package com.sosence.app.engine

enum class SafetyEventPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

sealed class SafetyEvent(
    val priority: SafetyEventPriority,
    val timestamp: Long = System.currentTimeMillis()
) {
    // 1. Battery Events
    data class LowBattery(
        val percentage: Int,
        val isCharging: Boolean = false,
        val temperatureCelsius: Float = 0f,
        val health: String = "Good"
    ) : SafetyEvent(SafetyEventPriority.HIGH)

    data class CriticalBattery(
        val percentage: Int,
        val isCharging: Boolean = false
    ) : SafetyEvent(SafetyEventPriority.CRITICAL)

    data class ChargingStarted(
        val percentage: Int
    ) : SafetyEvent(SafetyEventPriority.LOW)

    data class ChargingStopped(
        val percentage: Int
    ) : SafetyEvent(SafetyEventPriority.LOW)

    data class BatteryRecovered(
        val percentage: Int
    ) : SafetyEvent(SafetyEventPriority.LOW)

    // 2. Timer & Check-In Events
    data class SafetyTimerExpired(
        val reason: String,
        val totalMinutes: Int
    ) : SafetyEvent(SafetyEventPriority.HIGH)

    data class SafetyTimerReminder(
        val remainingMinutes: Int,
        val reason: String
    ) : SafetyEvent(SafetyEventPriority.NORMAL)

    data class CheckInExpired(
        val note: String
    ) : SafetyEvent(SafetyEventPriority.HIGH)

    // 3. Location & Journey Events
    data class JourneyDeviation(
        val latitude: Double,
        val longitude: Double,
        val deviationDistanceMeters: Double
    ) : SafetyEvent(SafetyEventPriority.HIGH)

    data class GeofenceEntered(
        val zoneId: String,
        val zoneName: String
    ) : SafetyEvent(SafetyEventPriority.NORMAL)

    data class GeofenceExited(
        val zoneId: String,
        val zoneName: String
    ) : SafetyEvent(SafetyEventPriority.HIGH)

    // 4. System & Recovery Events
    data class BootRestored(
        val restoredTimersCount: Int
    ) : SafetyEvent(SafetyEventPriority.LOW)

    data class ConnectivityChanged(
        val isConnected: Boolean,
        val networkType: String
    ) : SafetyEvent(SafetyEventPriority.LOW)

    // 5. Emergency SOS & Crisis Events
    data class FallDetected(
        val confidence: Float
    ) : SafetyEvent(SafetyEventPriority.CRITICAL)

    data class ManualSosTriggered(
        val triggerSource: String
    ) : SafetyEvent(SafetyEventPriority.CRITICAL)
}
