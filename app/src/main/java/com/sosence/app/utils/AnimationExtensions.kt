package com.sosence.app.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.sosence.app.R

/**
 * UI & Animation helper for SOSense application.
 * Provides micro-interactions (press bounce, pulse, radar, ripple, haptic feedback)
 * and macro-interactions (staggered screen entrance, activity slide & scale transitions).
 */
object AnimationExtensions {

    /**
     * Attaches an organic spring press micro-interaction to any view.
     */
    fun View.applyTouchBounce(scaleTo: Float = 0.96f, withHaptic: Boolean = true) {
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(scaleTo)
                        .scaleY(scaleTo)
                        .setDuration(100)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                    if (withHaptic) {
                        performLightHaptic(v.context)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(160)
                        .setInterpolator(OvershootInterpolator(2.0f))
                        .start()
                }
            }
            false
        }
    }

    /**
     * Staggered entrance animation for cards/lists (fade + slide up).
     */
    fun View.animateEntrance(delayMs: Long = 0, durationMs: Long = 320) {
        this.alpha = 0f
        this.translationY = 40f
        this.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(durationMs)
            .setStartDelay(delayMs)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /**
     * Rhythmic breathing/pulse animation for emergency SOS and active alert indicators.
     */
    fun View.startPulse(scale: Float = 1.08f, duration: Long = 750): ObjectAnimator {
        val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1.0f, scale).apply {
            this.duration = duration
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1.0f, scale).apply {
            this.duration = duration
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.start()
        return scaleX
    }

    /**
     * Shake animation for warning / alert validation errors.
     */
    fun View.shakeAlert() {
        val shake = AnimationUtils.loadAnimation(context, R.anim.shake_alert)
        startAnimation(shake)
    }

    /**
     * Gentle haptic tick on supported Android devices.
     */
    fun performLightHaptic(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(15)
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore if device doesn't have vibrator or permission
        }
    }

    /**
     * Standardized forward transition for opening activities.
     */
    @Suppress("DEPRECATION")
    fun Activity.applyForwardTransition() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        } else {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    /**
     * Standardized backward transition for closing activities.
     */
    @Suppress("DEPRECATION")
    fun Activity.applyBackwardTransition() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
