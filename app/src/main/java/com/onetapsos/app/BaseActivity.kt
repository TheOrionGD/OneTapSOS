package com.onetapsos.app

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.onetapsos.app.utils.AnimationExtensions.applyBackwardTransition

open class BaseActivity : AppCompatActivity() {

    lateinit var appSettings: AppSettings

    override fun attachBaseContext(newBase: Context) {
        val settings = AppSettings(newBase)
        super.attachBaseContext(settings.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSettings = AppSettings(this)

        setupSystemBarSeparation()

        // Smooth fade & slide transition
        window.decorView.alpha = 0f
        window.decorView.animate()
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applySystemBarInsets()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applySystemBarInsets()
    }

    private fun setupSystemBarSeparation() {
        try {
            val darkBg = ContextCompat.getColor(this, R.color.bg_dark_primary)
            window.statusBarColor = darkBg
            window.navigationBarColor = darkBg

            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applySystemBarInsets() {
        try {
            val content = findViewById<View>(android.R.id.content) ?: return
            ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
                val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                view.setPadding(0, statusBars.top, 0, 0)
                insets
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
