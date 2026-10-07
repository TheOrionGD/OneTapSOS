package com.sosence.app

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.sosence.app.engine.BackgroundSafetyEngine
import com.sosence.app.engine.SafetyEvent

class BatterySafetyActivity : BaseActivity() {

    private lateinit var tvPct: TextView
    private lateinit var tvHealth: TextView
    private lateinit var tvEngineStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery_safety)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        tvPct = findViewById(R.id.tvBatteryPct)
        tvHealth = findViewById(R.id.tvBatteryHealth)
        tvEngineStatus = findViewById(R.id.tvBatteryEngineStatus)

        findViewById<TextView>(R.id.tvBatteryDiagLink).setOnClickListener {
            startActivity(Intent(this, BackgroundDiagnosticsActivity::class.java))
        }

        findViewById<View>(R.id.btnConfigBatteryAlert).setOnClickListener {
            startActivity(Intent(this, BatteryAlertSettingsActivity::class.java))
        }

        findViewById<View>(R.id.btnSimulate5PctAlert).setOnClickListener {
            val threshold = appSettings.lowBatteryThreshold
            BackgroundSafetyEngine.dispatchEvent(
                this,
                SafetyEvent.LowBattery(percentage = threshold, isCharging = false, health = "Good")
            )
            Toast.makeText(this, "Simulated $threshold% low battery event dispatched!", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnOpenDiagnostics).setOnClickListener {
            startActivity(Intent(this, BackgroundDiagnosticsActivity::class.java))
        }

        refreshBatteryInfo()
    }

    private fun refreshBatteryInfo() {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, intentFilter)

        if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = if (level >= 0 && scale > 0) ((level * 100) / scale) else 0

            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val tempTenths = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val tempCelsius = tempTenths / 10.0f
            val healthCode = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

            val health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                else -> "Normal"
            }

            tvPct.text = "🔋 $pct%"
            tvPct.setTextColor(getColor(if (pct <= appSettings.lowBatteryThreshold) R.color.warning_yellow else R.color.success_green))
            tvHealth.text = "Status: ${if (isCharging) "Charging ⚡" else "Discharging"} • Health: $health • ${tempCelsius.toInt()}°C"
        }

        val threshold = appSettings.lowBatteryThreshold
        val isEnabled = appSettings.isBatteryAlertEnabled && appSettings.isBackgroundSafetyEnabled
        tvEngineStatus.text = if (isEnabled) "● Background Monitoring Active ($threshold% Threshold)" else "✕ Background Monitoring Disabled"
        tvEngineStatus.setTextColor(getColor(if (isEnabled) R.color.success_green else R.color.danger_red))
    }

    override fun onResume() {
        super.onResume()
        refreshBatteryInfo()
    }
}
