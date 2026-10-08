package com.onetapsos.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class PrivacySettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_settings)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<CardView>(R.id.btnSavePrivacy).setOnClickListener {
            Toast.makeText(this, "Privacy controls updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
