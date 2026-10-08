package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.onetapsos.app.data.AppDatabaseHelper
import com.onetapsos.app.engine.BackgroundSafetyEngine

class SafetyTimerActiveActivity : BaseActivity() {

    private var countdownTimer: CountDownTimer? = null
    private var millisLeft: Long = 30 * 60 * 1000L
    private lateinit var tvCountdown: TextView
    private lateinit var dbHelper: AppDatabaseHelper
    private var currentReason: String = "Safety Monitoring"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_timer_active)

        dbHelper = AppDatabaseHelper(this)

        val minutes = intent.getIntExtra("TIMER_MINUTES", 30)
        currentReason = intent.getStringExtra("TIMER_REASON") ?: appSettings.activeSafetyTimerReason

        // Check if there is already an active timer end time saved
        val now = System.currentTimeMillis()
        val savedEndTime = appSettings.activeSafetyTimerEndTime

        if (savedEndTime > now) {
            millisLeft = savedEndTime - now
        } else {
            millisLeft = minutes * 60 * 1000L
            val initialMins = (millisLeft / 60000).toInt().coerceAtLeast(1)
            BackgroundSafetyEngine.scheduleSafetyTimer(this, initialMins, currentReason)
        }

        findViewById<TextView>(R.id.tvActiveTimerReason).text = "Activity: $currentReason"
        tvCountdown = findViewById(R.id.tvTimerCountdown)

        findViewById<CardView>(R.id.btnTimerImSafe).setOnClickListener {
            stopAndCompleteTimer()
        }

        findViewById<CardView>(R.id.btnExtend15Mins).setOnClickListener {
            millisLeft += 15 * 60 * 1000L
            val newMins = (millisLeft / 60000).toInt().coerceAtLeast(1)
            BackgroundSafetyEngine.scheduleSafetyTimer(this, newMins, currentReason)
            startCountdown()
            Toast.makeText(this, "Timer extended by 15 minutes", Toast.LENGTH_SHORT).show()
        }

        findViewById<CardView>(R.id.btnStopTimer).setOnClickListener {
            cancelActiveTimer()
        }

        startCountdown()
    }

    private fun startCountdown() {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(millisLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                millisLeft = millisUntilFinished
                val totalSecs = millisUntilFinished / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                tvCountdown.text = "%02d:%02d".format(mins, secs)
            }

            override fun onFinish() {
                // Background alarm also fires, but if user has UI open, handle smoothly
                BackgroundSafetyEngine.cancelSafetyTimer(this@SafetyTimerActiveActivity)
                Toast.makeText(this@SafetyTimerActiveActivity, "⚠️ Safety Timer Expired!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@SafetyTimerActiveActivity, SOSActivationActivity::class.java))
                finish()
            }
        }.start()
    }

    private fun stopAndCompleteTimer() {
        countdownTimer?.cancel()
        BackgroundSafetyEngine.cancelSafetyTimer(this)
        dbHelper.recordCheckIn("Completed", "Safety timer completed: $currentReason")
        Toast.makeText(this, "✅ Safety confirmed! Timer stopped.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun cancelActiveTimer() {
        countdownTimer?.cancel()
        BackgroundSafetyEngine.cancelSafetyTimer(this)
        Toast.makeText(this, "Safety timer stopped", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }
}
