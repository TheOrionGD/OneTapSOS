package com.onetapsos.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.onetapsos.app.data.AppDatabaseHelper

class SafetyDashboardActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private var holdTimer: CountDownTimer? = null
    private val holdDurationMs = 3000L
    private var vibrator: Vibrator? = null
    private var glowAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_dashboard)

        dbHelper = AppDatabaseHelper(this)
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        val glow = findViewById<View>(R.id.viewDashboardGlow)
        glowAnimator = ObjectAnimator.ofFloat(glow, "alpha", 0.3f, 1.0f).apply {
            duration = 900
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }

        val frameSOS = findViewById<FrameLayout>(R.id.frameMainSOS)
        frameSOS.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
                    vibrateHaptic(80)
                    startHoldTimer()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    cancelHoldTimer()
                    true
                }
                else -> false
            }
        }



        findViewById<View>(R.id.btnSettingsShortcut).setOnClickListener {
            startActivity(Intent(this, SOSSettingsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardNavContacts).setOnClickListener {
            startActivity(Intent(this, TrustedContactsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardNavMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        findViewById<CardView>(R.id.cardNavUnsafe).setOnClickListener {
            startActivity(Intent(this, UnsafeSituationActivity::class.java))
        }

        findViewById<CardView>(R.id.cardNavTimer).setOnClickListener {
            startActivity(Intent(this, SafetyTimerActivity::class.java))
        }

        findViewById<CardView>(R.id.cardNavHelp).setOnClickListener {
            startActivity(Intent(this, NearbyHelpActivity::class.java))
        }

        findViewById<CardView>(R.id.cardNavAI).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        findViewById<CardView>(R.id.cardAllFeatures).setOnClickListener {
            startActivity(Intent(this, HelpAndSupportActivity::class.java))
        }

        findViewById<CardView>(R.id.cardDashImSafe).setOnClickListener {
            if (appSettings.isSosActive) {
                startActivity(Intent(this, SOSActiveActivity::class.java))
            } else {
                Toast.makeText(this, "No active SOS session.", Toast.LENGTH_SHORT).show()
            }
        }

        updateDashboardState()
    }

    override fun onResume() {
        super.onResume()
        updateDashboardState()
    }

    private fun updateDashboardState() {
        val contacts = dbHelper.getAllContacts()
        findViewById<TextView>(R.id.tvDashContactCount).text = "${contacts.size} Contact(s)"

        val tvState = findViewById<TextView>(R.id.tvSosButtonStateText)
        val tvBadge = findViewById<TextView>(R.id.tvSafetyStateBadge)
        val cardImSafe = findViewById<CardView>(R.id.cardDashImSafe)

        if (appSettings.isSosActive) {
            tvState.text = "STATE: EMERGENCY ACTIVE 🚨"
            tvBadge.text = "🚨 EMERGENCY IN PROGRESS"
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.danger_red))
            cardImSafe.visibility = View.VISIBLE
        } else {
            tvState.text = "STATE: READY"
            tvBadge.text = "🟢 System Ready • Armed"
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            cardImSafe.visibility = View.GONE
        }
    }

    private fun startHoldTimer() {
        holdTimer?.cancel()
        holdTimer = object : CountDownTimer(holdDurationMs, 200) {
            override fun onTick(millisUntilFinished: Long) {
                vibrateHaptic(40)
            }
            override fun onFinish() {
                vibrateHaptic(250)
                startActivity(Intent(this@SafetyDashboardActivity, SOSCountdownActivity::class.java))
            }
        }.start()
    }

    private fun cancelHoldTimer() {
        holdTimer?.cancel()
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

    override fun onDestroy() {
        super.onDestroy()
        holdTimer?.cancel()
        glowAnimator?.cancel()
    }
}
