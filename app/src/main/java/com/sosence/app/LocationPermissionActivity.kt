package com.sosence.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class LocationPermissionActivity : BaseActivity() {

    private val requestPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { updateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        updateStatus()

        findViewById<CardView>(R.id.btnGrantLocationPermission).setOnClickListener {
            requestPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun updateStatus() {
        val hasLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val tvStatus = findViewById<TextView>(R.id.tvLocationPermissionStatus)

        tvStatus.text = "Location Permission Status: ${if (hasLoc) "GRANTED ✅" else "NOT GRANTED ❌"}"
        tvStatus.setTextColor(ContextCompat.getColor(this, if (hasLoc) R.color.success_green else R.color.danger_red))
    }
}
