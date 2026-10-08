package com.onetapsos.app.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

class BatteryEventReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BatteryEventReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Battery event broadcast received: $action")

        val pendingResult = goAsync()

        try {
            // Read sticky battery metrics
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.applicationContext.registerReceiver(null, batteryFilter)

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val healthCode = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
            val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0

            val pct = if (level >= 0 && scale > 0) ((level * 100) / scale) else 5
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val tempCelsius = tempTenths / 10.0f
            val health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                else -> "Normal"
            }

            when (action) {
                Intent.ACTION_BATTERY_LOW -> {
                    BackgroundSafetyEngine.dispatchEvent(
                        context,
                        SafetyEvent.LowBattery(pct, isCharging, tempCelsius, health)
                    )
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    BackgroundSafetyEngine.dispatchEvent(
                        context,
                        SafetyEvent.BatteryRecovered(pct)
                    )
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    BackgroundSafetyEngine.dispatchEvent(
                        context,
                        SafetyEvent.ChargingStarted(pct)
                    )
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    BackgroundSafetyEngine.dispatchEvent(
                        context,
                        SafetyEvent.ChargingStopped(pct)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling battery broadcast: ${e.message}", e)
        } finally {
            pendingResult.finish()
        }
    }
}
