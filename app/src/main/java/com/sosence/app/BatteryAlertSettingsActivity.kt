package com.sosence.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView

class BatteryAlertSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery_alert_settings)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val switchBattery = findViewById<SwitchCompat>(R.id.switchBattery5Pct)
        switchBattery.isChecked = appSettings.isBatteryAlertEnabled

        findViewById<CardView>(R.id.btnSaveBatterySettings).setOnClickListener {
            appSettings.isBatteryAlertEnabled = switchBattery.isChecked
            Toast.makeText(this, "Battery alert settings updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
