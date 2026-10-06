package com.sosence.app

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.sosence.app.utils.AnimationExtensions.applyBackwardTransition
import com.sosence.app.utils.AnimationExtensions.applyForwardTransition

open class BaseActivity : AppCompatActivity() {

    lateinit var appSettings: AppSettings

    override fun attachBaseContext(newBase: Context) {
        val settings = AppSettings(newBase)
        super.attachBaseContext(settings.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSettings = AppSettings(this)

        // Smooth fade & slide transition
        window.decorView.alpha = 0f
        window.decorView.animate()
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    override fun finish() {
        super.finish()
        applyBackwardTransition()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun setupHeaderBack(toolbarId: Int? = null) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }
}

