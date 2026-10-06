package com.sosence.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class CheckInActivity : BaseActivity() {

    private var checkInTimer: CountDownTimer? = null
    private var isCheckInActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)
        animateEntrance()
        createNotificationChannel()

        val btnBack = findViewById<ImageView>(R.id.ivCheckInBack)
        val btnStart = findViewById<CardView>(R.id.btnStartCheckIn)
        val btnCancel = findViewById<CardView>(R.id.btnCancelCheckIn)
        val tvStatus = findViewById<TextView>(R.id.tvCheckInStatus)
        val tvCountdown = findViewById<TextView>(R.id.tvCheckInCountdown)
        val spinnerTime = findViewById<Spinner>(R.id.spinnerCheckInTime)
        val etNote = findViewById<EditText>(R.id.etCheckInNote)

        val times = arrayOf("5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "2 hours")
        val minutesMap = mapOf("5 minutes" to 5, "10 minutes" to 10, "15 minutes" to 15,
            "30 minutes" to 30, "1 hour" to 60, "2 hours" to 120)
        spinnerTime.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, times)

        btnBack.setOnClickListener { finish() }

        btnStart.setOnClickListener {
            if (isCheckInActive) { Toast.makeText(this, "Check-in already active", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val selected = spinnerTime.selectedItem.toString()
            val minutes = minutesMap[selected] ?: 15
            val totalMs = minutes * 60 * 1000L
            isCheckInActive = true
            tvStatus.text = "✅ Check-in active"
            btnCancel.visibility = android.view.View.VISIBLE

            checkInTimer = object : CountDownTimer(totalMs, 1000) {
                override fun onTick(ms: Long) {
                    val m = ms / 60000; val s = (ms % 60000) / 1000
                    tvCountdown.text = String.format(java.util.Locale.US, "Time remaining: %02d:%02d", m, s)
                }
                override fun onFinish() {
                    isCheckInActive = false
                    tvStatus.text = "⚠️ Check-in expired! SOS triggered."
                    tvCountdown.text = ""
                    btnCancel.visibility = android.view.View.GONE
                    showMissedCheckInNotification()
                }
            }.start()
        }

        btnCancel.setOnClickListener {
            checkInTimer?.cancel()
            isCheckInActive = false
            tvStatus.text = "Check-in cancelled. You're safe!"
            tvCountdown.text = ""
            btnCancel.visibility = android.view.View.GONE
        }
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("check_in", "Check-In Alerts", NotificationManager.IMPORTANCE_HIGH)
                getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            }
        } catch (e: Exception) {}
    }

    private fun showMissedCheckInNotification() {
        try {
            SOSNotificationManager.showMissedCheckInNotification(this, "Scheduled Check-In")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        checkInTimer?.cancel()
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}
