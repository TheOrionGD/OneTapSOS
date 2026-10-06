package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView

class NearbyHelpActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_help)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val openMap = { startActivity(Intent(this, MapActivity::class.java)) }
        findViewById<CardView>(R.id.cardPolice).setOnClickListener { openMap() }
        findViewById<CardView>(R.id.cardHospital).setOnClickListener { openMap() }
    }
}
