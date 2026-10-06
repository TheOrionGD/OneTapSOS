package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class SOSSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_settings)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val rb15 = findViewById<RadioButton>(R.id.rb15Sec)
        val rb5 = findViewById<RadioButton>(R.id.rb5Sec)

        if (appSettings.sosTimerSeconds == 15) {
            rb15.isChecked = true
        } else {
            rb5.isChecked = true
        }

        findViewById<CardView>(R.id.btnEditSafeMsgShortcut).setOnClickListener {
            startActivity(Intent(this, ImSafeMessageActivity::class.java))
        }

        findViewById<CardView>(R.id.btnSaveSosSettings).setOnClickListener {
            appSettings.sosTimerSeconds = if (rb15.isChecked) 15 else 5
            Toast.makeText(this, "SOS preferences saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
