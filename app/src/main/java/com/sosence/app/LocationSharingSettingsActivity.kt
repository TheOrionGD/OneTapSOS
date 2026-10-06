package com.sosence.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class LocationSharingSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_sharing_settings)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<CardView>(R.id.btnSaveLocSettings).setOnClickListener {
            Toast.makeText(this, "Location sharing preferences saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
