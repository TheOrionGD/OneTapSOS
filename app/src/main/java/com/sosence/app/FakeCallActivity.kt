package com.sosence.app

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class FakeCallActivity : AppCompatActivity() {

    private var ringTimer: CountDownTimer? = null
    private var callTimer: CountDownTimer? = null
    private var isCallActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_call)

        val btnBack = findViewById<ImageView>(R.id.ivFakeCallBack)
        val btnStart = findViewById<CardView>(R.id.btnStartFakeCall)
        val btnAnswer = findViewById<CardView>(R.id.btnAnswerCall)
        val btnDecline = findViewById<CardView>(R.id.btnDeclineCall)
        val tvStatus = findViewById<TextView>(R.id.tvFakeCallStatus)
        val tvTimer = findViewById<TextView>(R.id.tvFakeCallTimer)
        val layoutRinging = findViewById<android.view.View>(R.id.layoutRinging)
        val layoutSetup = findViewById<android.view.View>(R.id.layoutSetup)

        btnBack.setOnClickListener { finish() }

        btnStart.setOnClickListener {
            layoutSetup.visibility = android.view.View.GONE
            layoutRinging.visibility = android.view.View.VISIBLE
            tvStatus.text = "📞 Incoming call..."
            startRinging()
        }

        btnAnswer.setOnClickListener {
            ringTimer?.cancel()
            isCallActive = true
            tvStatus.text = "Connected — speak naturally"
            btnAnswer.visibility = android.view.View.GONE
            var seconds = 0
            callTimer = object : CountDownTimer(300_000, 1000) {
                override fun onTick(ms: Long) {
                    seconds++
                    val m = seconds / 60; val s = seconds % 60
                    tvTimer.text = String.format("%02d:%02d", m, s)
                }
                override fun onFinish() { endCall(layoutRinging, layoutSetup, tvStatus, tvTimer, btnAnswer) }
            }.start()
        }

        btnDecline.setOnClickListener {
            ringTimer?.cancel()
            callTimer?.cancel()
            endCall(layoutRinging, layoutSetup, tvStatus, tvTimer, btnAnswer)
        }
    }

    private fun startRinging() {
        ringTimer = object : CountDownTimer(30_000, 1000) {
            override fun onTick(ms: Long) {}
            override fun onFinish() { finish() }
        }.start()
    }

    private fun endCall(layoutRinging: android.view.View, layoutSetup: android.view.View,
                        tvStatus: TextView, tvTimer: TextView, btnAnswer: CardView) {
        layoutRinging.visibility = android.view.View.GONE
        layoutSetup.visibility = android.view.View.VISIBLE
        tvStatus.text = "Call ended"
        tvTimer.text = "00:00"
        btnAnswer.visibility = android.view.View.VISIBLE
        isCallActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        ringTimer?.cancel()
        callTimer?.cancel()
    }
}
