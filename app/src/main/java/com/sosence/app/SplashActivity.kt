package com.sosence.app

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.bg_dark_primary)
            window.navigationBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.bg_dark_primary)
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        } catch (e: Exception) {}

        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.ivSplashLogo)
        val tagline = findViewById<TextView>(R.id.tvSplashTagline)
        val title = findViewById<TextView>(R.id.tvSplashTitle)
        val trustPill = findViewById<View>(R.id.llSplashPill)
        val glowOuter = findViewById<View>(R.id.viewSplashGlowOuter)
        val glowInner = findViewById<View>(R.id.viewSplashGlow)

        // Pulsing radar animations
        ObjectAnimator.ofPropertyValuesHolder(
            glowOuter,
            PropertyValuesHolder.ofFloat("scaleX", 0.9f, 1.25f),
            PropertyValuesHolder.ofFloat("scaleY", 0.9f, 1.25f),
            PropertyValuesHolder.ofFloat("alpha", 0.12f, 0.35f)
        ).apply {
            duration = 1400
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofPropertyValuesHolder(
            glowInner,
            PropertyValuesHolder.ofFloat("scaleX", 0.95f, 1.15f),
            PropertyValuesHolder.ofFloat("scaleY", 0.95f, 1.15f),
            PropertyValuesHolder.ofFloat("alpha", 0.2f, 0.45f)
        ).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Staggered animated reveals
        logo.scaleX = 0.7f
        logo.scaleY = 0.7f
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator())
            .start()

        title.translationY = 30f
        title.postDelayed({
            title.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 200)

        tagline.translationY = 20f
        tagline.postDelayed({
            tagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 400)

        trustPill.translationY = 15f
        trustPill.postDelayed({
            trustPill.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 600)

        val prefs = getSharedPreferences("sosense_prefs", MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboarding_done", false)

        splashHandler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                val next = if (onboardingDone) {
                    Intent(this, MainActivity::class.java)
                } else {
                    Intent(this, OnboardingActivity::class.java)
                }
                startActivity(next)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }, 2300)
    }

    override fun onDestroy() {
        super.onDestroy()
        splashHandler.removeCallbacksAndMessages(null)
    }
}
