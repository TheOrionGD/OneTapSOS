package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class SafeRouteActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_route)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val etDest = findViewById<EditText>(R.id.etDestination)

        findViewById<CardView>(R.id.btnFindSafeRoute).setOnClickListener {
            val dest = etDest.text.toString().trim()
            if (dest.isEmpty()) {
                Toast.makeText(this, "Please enter destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Safe route calculated on OpenStreetMap", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MapActivity::class.java))
        }
    }
}
