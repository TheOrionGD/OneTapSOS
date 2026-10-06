package com.sosence.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView

class NotificationSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)
        animateEntrance()

        val prefs = getSharedPreferences("sosense_prefs", MODE_PRIVATE)
        val btnBack = findViewById<ImageView>(R.id.ivNotifSettingsBack)
        val switchSOS = findViewById<SwitchCompat>(R.id.switchSOSAlerts)
        val switchCheckIn = findViewById<SwitchCompat>(R.id.switchCheckInAlerts)
        val switchBattery = findViewById<SwitchCompat>(R.id.switchBatteryNotif)
        val switchFall = findViewById<SwitchCompat>(R.id.switchFallNotif)
        val switchSafetyTips = findViewById<SwitchCompat>(R.id.switchSafetyTipsNotif)
        val btnSave = findViewById<CardView>(R.id.btnSaveNotifSettings)

        switchSOS.isChecked = prefs.getBoolean("notif_sos", true)
        switchCheckIn.isChecked = prefs.getBoolean("notif_checkin", true)
        switchBattery.isChecked = prefs.getBoolean("notif_battery", true)
        switchFall.isChecked = prefs.getBoolean("notif_fall", true)
        switchSafetyTips.isChecked = prefs.getBoolean("notif_tips", false)

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            prefs.edit()
                .putBoolean("notif_sos", switchSOS.isChecked)
                .putBoolean("notif_checkin", switchCheckIn.isChecked)
                .putBoolean("notif_battery", switchBattery.isChecked)
                .putBoolean("notif_fall", switchFall.isChecked)
                .putBoolean("notif_tips", switchSafetyTips.isChecked)
                .apply()
            Toast.makeText(this, "Notification settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}
