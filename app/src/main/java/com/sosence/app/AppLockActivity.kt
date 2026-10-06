package com.sosence.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat

class AppLockActivity : AppCompatActivity() {

    private val correctPin: String get() = getSharedPreferences("sosense_prefs", MODE_PRIVATE).getString("app_pin", "") ?: ""
    private var enteredPin = StringBuilder()
    private var isSettingPin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock)
        animateEntrance()

        val btnBack = findViewById<ImageView>(R.id.ivLockBack)
        val tvPinDisplay = findViewById<TextView>(R.id.tvPinDisplay)
        val tvLockTitle = findViewById<TextView>(R.id.tvLockTitle)
        val btnSetPin = findViewById<CardView>(R.id.btnSetPin)
        val btnClearPin = findViewById<CardView>(R.id.btnClearPin)

        isSettingPin = correctPin.isEmpty()
        tvLockTitle.text = if (isSettingPin) "Set your 4-digit PIN" else "App Lock Settings"

        btnBack.setOnClickListener { finish() }

        val numButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        numButtons.forEachIndexed { index, resId ->
            findViewById<CardView>(resId).setOnClickListener {
                if (enteredPin.length < 4) {
                    enteredPin.append(index)
                    tvPinDisplay.text = "●".repeat(enteredPin.length) + "○".repeat(4 - enteredPin.length)
                    if (enteredPin.length == 4) processPin(tvPinDisplay, tvLockTitle)
                }
            }
        }

        val btnDel = findViewById<CardView>(R.id.btnDel)
        btnDel.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                tvPinDisplay.text = "●".repeat(enteredPin.length) + "○".repeat(4 - enteredPin.length)
            }
        }

        btnSetPin.setOnClickListener {
            isSettingPin = true
            enteredPin.clear()
            tvPinDisplay.text = "○○○○"
            tvLockTitle.text = "Enter new 4-digit PIN"
        }

        btnClearPin.setOnClickListener {
            getSharedPreferences("sosense_prefs", MODE_PRIVATE).edit().remove("app_pin").apply()
            Toast.makeText(this, "App lock PIN cleared", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun processPin(tvDisplay: TextView, tvTitle: TextView) {
        val pin = enteredPin.toString()
        if (isSettingPin) {
            getSharedPreferences("sosense_prefs", MODE_PRIVATE).edit().putString("app_pin", pin).apply()
            Toast.makeText(this, "✅ PIN set successfully!", Toast.LENGTH_SHORT).show()
            isSettingPin = false
            tvTitle.text = "App Lock Settings"
        }
        enteredPin.clear()
        tvDisplay.text = "○○○○"
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}
