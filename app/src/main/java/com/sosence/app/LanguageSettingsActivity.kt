package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class LanguageSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_settings)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val rbEn = findViewById<RadioButton>(R.id.rbEnglish)
        val rbTa = findViewById<RadioButton>(R.id.rbTamil)
        val rbHi = findViewById<RadioButton>(R.id.rbHindi)

        when (appSettings.languageCode) {
            "ta" -> rbTa.isChecked = true
            "hi" -> rbHi.isChecked = true
            else -> rbEn.isChecked = true
        }

        findViewById<CardView>(R.id.btnApplyLanguage).setOnClickListener {
            val selectedCode = when {
                rbTa.isChecked -> "ta"
                rbHi.isChecked -> "hi"
                else -> "en"
            }

            appSettings.languageCode = selectedCode
            Toast.makeText(this, "Language updated to $selectedCode", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, SafetyDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
