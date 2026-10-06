package com.sosence.app

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView

class BatterySafetyActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery_safety)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val tvPct = findViewById<TextView>(R.id.tvBatteryPct)
        val tvHealth = findViewById<TextView>(R.id.tvBatteryHealth)

        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, intentFilter)

        if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = (level * 100) / scale.toFloat()

            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            tvPct.text = "🔋 %.0f%%".format(pct)
            tvHealth.text = "Battery Status: ${if (isCharging) "Charging ⚡" else "Discharging"} • Health: Good"
        }

        findViewById<CardView>(R.id.btnConfigBatteryAlert).setOnClickListener {
            startActivity(Intent(this, BatteryAlertSettingsActivity::class.java))
        }
    }
}
