package com.sosence.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class LocationPermissionActivity : BaseActivity() {

    private val locationResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { updateStatus() }

    private val requestPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        updateStatus()

        findViewById<CardView>(R.id.btnGrantLocationPermission).setOnClickListener {
            if (!com.sosence.app.utils.LocationHelper.hasLocationPermission(this)) {
                requestPermLauncher.launch(com.sosence.app.utils.LocationHelper.LOCATION_PERMISSIONS)
            } else if (!com.sosence.app.utils.LocationHelper.isGpsOrNetworkEnabled(this)) {
                com.sosence.app.utils.LocationHelper.promptEnableLocation(this, locationResolutionLauncher) {
                    updateStatus()
                }
            } else {
                updateStatus()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val hasLoc = com.sosence.app.utils.LocationHelper.hasLocationPermission(this)
        val isGpsOn = com.sosence.app.utils.LocationHelper.isGpsOrNetworkEnabled(this)
        val tvStatus = findViewById<TextView>(R.id.tvLocationPermissionStatus)

        val statusText = when {
            hasLoc && isGpsOn -> "Permission: GRANTED ✅ | GPS: ACTIVE 🟢"
            hasLoc && !isGpsOn -> "Permission: GRANTED ✅ | GPS: OFF ⚠️ (Tap to Enable)"
            else -> "Permission: NOT GRANTED ❌ | GPS: UNKNOWN"
        }
        tvStatus.text = statusText
        tvStatus.setTextColor(ContextCompat.getColor(this, if (hasLoc && isGpsOn) R.color.success_green else if (hasLoc) R.color.warning_yellow else R.color.danger_red))
    }
}
