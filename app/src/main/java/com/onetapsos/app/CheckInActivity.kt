package com.onetapsos.app

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.onetapsos.app.data.AppDatabaseHelper
import com.onetapsos.app.engine.BackgroundSafetyEngine
import java.util.Locale

class CheckInActivity : BaseActivity() {

    private var checkInTimer: CountDownTimer? = null
    private var isCheckInActive = false
    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)
        animateEntrance()

        dbHelper = AppDatabaseHelper(this)

        val btnBack = findViewById<ImageView>(R.id.ivCheckInBack)
        val btnStart = findViewById<CardView>(R.id.btnStartCheckIn)
        val btnCancel = findViewById<CardView>(R.id.btnCancelCheckIn)
        val tvStatus = findViewById<TextView>(R.id.tvCheckInStatus)
        val tvCountdown = findViewById<TextView>(R.id.tvCheckInCountdown)
        val spinnerTime = findViewById<Spinner>(R.id.spinnerCheckInTime)
        val etNote = findViewById<EditText>(R.id.etCheckInNote)

        val times = arrayOf("5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "2 hours")
        val minutesMap = mapOf(
            "5 minutes" to 5, "10 minutes" to 10, "15 minutes" to 15,
            "30 minutes" to 30, "1 hour" to 60, "2 hours" to 120
        )
        spinnerTime.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, times)

        btnBack.setOnClickListener { finish() }

        // Check if checkin was already scheduled
        val now = System.currentTimeMillis()
        val savedEndTime = appSettings.activeCheckInEndTime
        if (savedEndTime > now) {
            val remainingMs = savedEndTime - now
            isCheckInActive = true
            tvStatus.text = "✅ Check-in active (Background Armed)"
            btnCancel.visibility = android.view.View.VISIBLE
            startUiCountdown(remainingMs, tvCountdown, tvStatus, btnCancel)
        }

        btnStart.setOnClickListener {
            if (isCheckInActive) {
                Toast.makeText(this, "Check-in is already active", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selected = spinnerTime.selectedItem.toString()
            val minutes = minutesMap[selected] ?: 15
            val note = etNote.text.toString().trim().ifEmpty { "Safety Check-In ($minutes mins)" }
            val totalMs = minutes * 60 * 1000L

            com.onetapsos.app.engine.BackgroundPermissionHelper.checkExactAlarmWithRationale(this) {
                isCheckInActive = true
                tvStatus.text = "✅ Check-in active"
                btnCancel.visibility = android.view.View.VISIBLE

                // Schedule with exact background AlarmManager
                BackgroundSafetyEngine.scheduleCheckIn(this, minutes, note)
                Toast.makeText(this, "Check-in armed for $minutes mins. You may lock your phone.", Toast.LENGTH_LONG).show()

                startUiCountdown(totalMs, tvCountdown, tvStatus, btnCancel)
            }
        }

        btnCancel.setOnClickListener {
            checkInTimer?.cancel()
            isCheckInActive = false
            BackgroundSafetyEngine.cancelCheckIn(this)
            dbHelper.recordCheckIn("Cancelled", "User confirmed safety before expiration")
            tvStatus.text = "Check-in completed. You're safe!"
            tvCountdown.text = ""
            btnCancel.visibility = android.view.View.GONE
            Toast.makeText(this, "Check-in completed successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startUiCountdown(
        totalMs: Long,
        tvCountdown: TextView,
        tvStatus: TextView,
        btnCancel: CardView
    ) {
        checkInTimer?.cancel()
        checkInTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(ms: Long) {
                val m = ms / 60000
                val s = (ms % 60000) / 1000
                tvCountdown.text = String.format(Locale.US, "Time remaining: %02d:%02d", m, s)
            }

            override fun onFinish() {
                isCheckInActive = false
                tvStatus.text = "⚠️ Check-in expired! SOS notification dispatched."
                tvCountdown.text = ""
                btnCancel.visibility = android.view.View.GONE
            }
        }.start()
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
