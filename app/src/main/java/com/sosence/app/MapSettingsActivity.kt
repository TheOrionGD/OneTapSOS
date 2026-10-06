package com.sosence.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class MapSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_settings)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<CardView>(R.id.btnSaveMapSettings).setOnClickListener {
            Toast.makeText(this, "Map preferences saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
