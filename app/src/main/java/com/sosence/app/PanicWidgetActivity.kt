package com.sosence.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class PanicWidgetActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panic_widget)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<CardView>(R.id.btnSetupWidget).setOnClickListener {
            Toast.makeText(this, "Quick Settings SOS Tile & Panic Widget active", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
