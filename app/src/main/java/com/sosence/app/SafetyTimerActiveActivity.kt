package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.sosence.app.data.AppDatabaseHelper

class SafetyTimerActiveActivity : BaseActivity() {

    private var countdownTimer: CountDownTimer? = null
    private var millisLeft: Long = 30 * 60 * 1000L
    private lateinit var tvCountdown: TextView
    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_timer_active)

        dbHelper = AppDatabaseHelper(this)
        val minutes = intent.getIntExtra("TIMER_MINUTES", 30)
        val reason = intent.getStringExtra("TIMER_REASON") ?: "Safety Monitoring"

        millisLeft = minutes * 60 * 1000L
        findViewById<TextView>(R.id.tvActiveTimerReason).text = "Activity: $reason"
        tvCountdown = findViewById(R.id.tvTimerCountdown)

        findViewById<CardView>(R.id.btnTimerImSafe).setOnClickListener {
            countdownTimer?.cancel()
            SOSNotificationManager.cancelSafetyTimerNotification(this)
            dbHelper.recordCheckIn("Completed", "Safety timer completed: $reason")
            Toast.makeText(this, "✅ Safety confirmed!", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<CardView>(R.id.btnExtend15Mins).setOnClickListener {
            millisLeft += 15 * 60 * 1000L
            startCountdown(reason)
            Toast.makeText(this, "Timer extended by 15 minutes", Toast.LENGTH_SHORT).show()
        }

        findViewById<CardView>(R.id.btnStopTimer).setOnClickListener {
            countdownTimer?.cancel()
            SOSNotificationManager.cancelSafetyTimerNotification(this)
            Toast.makeText(this, "Safety timer stopped", Toast.LENGTH_SHORT).show()
            finish()
        }

        startCountdown(reason)
    }

    private fun startCountdown(reason: String) {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(millisLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                millisLeft = millisUntilFinished
                val totalSecs = millisUntilFinished / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                val formatted = "%02d:%02d".format(mins, secs)
                tvCountdown.text = formatted
            }

            override fun onFinish() {
                SOSNotificationManager.cancelSafetyTimerNotification(this@SafetyTimerActiveActivity)
                SOSNotificationManager.showMissedCheckInNotification(this@SafetyTimerActiveActivity, reason)
                dbHelper.recordCheckIn("Missed", "Safety timer expired without confirmation")
                Toast.makeText(this@SafetyTimerActiveActivity, "⚠️ Timer Expired! Launching SOS...", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@SafetyTimerActiveActivity, SOSActivationActivity::class.java))
                finish()
            }
        }.start()

        val initialMins = millisLeft / 60000
        SOSNotificationManager.showSafetyTimerNotification(this, reason, "$initialMins mins remaining")
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }
}
