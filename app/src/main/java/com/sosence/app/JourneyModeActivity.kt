package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class JourneyModeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_mode)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<CardView>(R.id.btnStartJourney).setOnClickListener {
            Toast.makeText(this, "Journey monitoring active", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LocationShareActiveActivity::class.java))
        }
    }
}
