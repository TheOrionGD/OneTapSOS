package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial

class BatteryAlertSettingsActivity : BaseActivity() {

    private var selectedThreshold: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery_alert_settings)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvOpenDiag).setOnClickListener {
            startActivity(Intent(this, BackgroundDiagnosticsActivity::class.java))
        }

        val switchBg = findViewById<SwitchMaterial>(R.id.switchBackgroundSafety)
        val switchAlert = findViewById<SwitchMaterial>(R.id.switchBatteryAlert)
        val switchVib = findViewById<SwitchMaterial>(R.id.switchBatteryVibration)

        switchBg.isChecked = appSettings.isBackgroundSafetyEnabled
        switchAlert.isChecked = appSettings.isBatteryAlertEnabled
        switchVib.isChecked = appSettings.isBatteryVibrationEnabled

        switchBg.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                com.sosence.app.engine.BackgroundPermissionHelper.checkBatteryOptimizationWithRationale(this)
            }
        }

        switchAlert.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                com.sosence.app.engine.BackgroundPermissionHelper.checkNotificationWithRationale(this)
            }
        }

        selectedThreshold = appSettings.lowBatteryThreshold
        setupThresholdChips()

        findViewById<TextView>(R.id.btnSaveBatterySettings).setOnClickListener {
            appSettings.isBackgroundSafetyEnabled = switchBg.isChecked
            appSettings.isBatteryAlertEnabled = switchAlert.isChecked
            appSettings.isBatteryVibrationEnabled = switchVib.isChecked
            appSettings.lowBatteryThreshold = selectedThreshold

            if (switchAlert.isChecked && !com.sosence.app.engine.BackgroundPermissionHelper.isNotificationPermissionGranted(this)) {
                com.sosence.app.engine.BackgroundPermissionHelper.checkNotificationWithRationale(this)
            }

            Toast.makeText(this, "Battery alert settings updated ($selectedThreshold% threshold)", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupThresholdChips() {
        val chip5 = findViewById<TextView>(R.id.chipThreshold5)
        val chip10 = findViewById<TextView>(R.id.chipThreshold10)
        val chip15 = findViewById<TextView>(R.id.chipThreshold15)
        val chip20 = findViewById<TextView>(R.id.chipThreshold20)

        val chips = listOf(chip5 to 5, chip10 to 10, chip15 to 15, chip20 to 20)

        fun updateSelection(target: Int) {
            selectedThreshold = target
            chips.forEach { (view, value) ->
                val isSelected = value == target
                view.setBackgroundResource(if (isSelected) R.drawable.bg_pill_accent else R.drawable.bg_pill_neutral)
                view.setTextColor(getColor(if (isSelected) R.color.accent_cyan else R.color.text_primary))
            }
        }

        updateSelection(selectedThreshold)

        chip5.setOnClickListener { updateSelection(5) }
        chip10.setOnClickListener { updateSelection(10) }
        chip15.setOnClickListener { updateSelection(15) }
        chip20.setOnClickListener { updateSelection(20) }
    }
}
