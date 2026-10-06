package com.sosence.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.sosence.app.data.AppDatabaseHelper

class SOSActivationActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private var holdTimer: CountDownTimer? = null
    private val holdDurationMs = 3000L
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_activation)

        dbHelper = AppDatabaseHelper(this)
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        val btnBack = findViewById<TextView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val tvContactCount = findViewById<TextView>(R.id.tvContactCount)
        val contacts = dbHelper.getAllContacts()
        tvContactCount.text = "${contacts.size} Contact(s)"

        val tvCurrentLocation = findViewById<TextView>(R.id.tvCurrentLocation)
        val locations = dbHelper.getLocationHistory()
        if (locations.isNotEmpty()) {
            val loc = locations.first()
            tvCurrentLocation.text = "Lat: %.4f, Lng: %.4f".format(loc.latitude, loc.longitude)
        } else {
            tvCurrentLocation.text = "GPS Ready (High Accuracy)"
        }

        val frameSOSButton = findViewById<FrameLayout>(R.id.frameSOSButton)
        frameSOSButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(120).start()
                    vibrateHaptic(100)
                    startHoldTimer()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    cancelHoldTimer()
                    true
                }
                else -> false
            }
        }

        val btnQuickTap = findViewById<CardView>(R.id.btnQuickTap)
        btnQuickTap.setOnClickListener {
            vibrateHaptic(200)
            launchCountdownScreen()
        }
    }

    private fun startHoldTimer() {
        holdTimer?.cancel()
        holdTimer = object : CountDownTimer(holdDurationMs, 200) {
            override fun onTick(millisUntilFinished: Long) {
                vibrateHaptic(50)
            }
            override fun onFinish() {
                vibrateHaptic(300)
                launchCountdownScreen()
            }
        }.start()
    }

    private fun cancelHoldTimer() {
        holdTimer?.cancel()
    }

    private fun launchCountdownScreen() {
        startActivity(Intent(this, SOSCountdownActivity::class.java))
        finish()
    }

    private fun vibrateHaptic(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (e: Exception) {}
    }
}
