package com.sosence.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class LocationShareActiveActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_share_active)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<CardView>(R.id.btnStopSharing).setOnClickListener {
            Toast.makeText(this, "Live location sharing stopped.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
