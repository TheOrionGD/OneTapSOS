package com.onetapsos.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class NotificationSettingsActivity : BaseActivity() {

    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(this, "✅ Notification permission enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "⚠️ Notification permission denied. Alerts may not show.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)
        animateEntrance()

        checkAndRequestNotificationPermission()

        val btnBack = findViewById<ImageView>(R.id.ivNotifSettingsBack)
        val switchSOS = findViewById<SwitchCompat>(R.id.switchSOSAlerts)
        val switchFall = findViewById<SwitchCompat>(R.id.switchFallNotif)
        val switchCheckIn = findViewById<SwitchCompat>(R.id.switchCheckInAlerts)
        val switchJourney = findViewById<SwitchCompat>(R.id.switchJourneyNotif)
        val switchBattery = findViewById<SwitchCompat>(R.id.switchBatteryNotif)
        val switchSafeZone = findViewById<SwitchCompat>(R.id.switchSafeZoneNotif)
        val switchSafetyTips = findViewById<SwitchCompat>(R.id.switchSafetyTipsNotif)
        val btnSave = findViewById<CardView>(R.id.btnSaveNotifSettings)

        // Load settings
        switchSOS.isChecked = appSettings.isNotifSosEnabled
        switchFall.isChecked = appSettings.isNotifFallEnabled
        switchCheckIn.isChecked = appSettings.isNotifCheckInEnabled
        switchJourney.isChecked = appSettings.isNotifJourneyEnabled
        switchBattery.isChecked = appSettings.isNotifBatteryEnabled
        switchSafeZone.isChecked = appSettings.isNotifSafeZoneEnabled
        switchSafetyTips.isChecked = appSettings.isNotifTipsEnabled

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            appSettings.isNotifSosEnabled = switchSOS.isChecked
            appSettings.isNotifFallEnabled = switchFall.isChecked
            appSettings.isNotifCheckInEnabled = switchCheckIn.isChecked
            appSettings.isNotifJourneyEnabled = switchJourney.isChecked
            appSettings.isNotifBatteryEnabled = switchBattery.isChecked
            appSettings.isNotifSafeZoneEnabled = switchSafeZone.isChecked
            appSettings.isNotifTipsEnabled = switchSafetyTips.isChecked

            Toast.makeText(this, "✅ Notification preferences saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupNotificationTestingLab()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupNotificationTestingLab() {
        // Daily Safety Tip Test ONLY
        findViewById<CardView>(R.id.btnTestTipsNotif)?.setOnClickListener {
            SOSNotificationManager.showSafetyTipNotification(
                this,
                "Emergency Route Preparedness",
                "Before travelling after dark, memorize at least 2 well-lit alternative transit routes."
            )
            Toast.makeText(this, "💡 Dispatched Safety Tip notification!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}
