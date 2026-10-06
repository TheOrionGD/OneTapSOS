package com.sosence.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class LiveTrackingActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var trackingActive = false
    private var trackingTimer: CountDownTimer? = null
    private var lastLocation: Location? = null
    private val handler = Handler(Looper.getMainLooper())

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startTracking() else Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tracking)
        animateEntrance()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnBack = findViewById<ImageView>(R.id.ivTrackingBack)
        val btnToggle = findViewById<CardView>(R.id.btnToggleTracking)
        val tvToggleLabel = findViewById<TextView>(R.id.tvTrackingToggleLabel)
        val tvStatus = findViewById<TextView>(R.id.tvTrackingStatus)
        val tvLocation = findViewById<TextView>(R.id.tvTrackingLocation)
        val btnShare = findViewById<CardView>(R.id.btnShareTrackingLocation)

        btnBack.setOnClickListener { finish() }

        btnToggle.setOnClickListener {
            if (trackingActive) {
                stopTracking()
                tvToggleLabel.text = "Start Live Tracking"
                tvStatus.text = "Tracking stopped"
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    startTracking()
                    tvToggleLabel.text = "Stop Tracking"
                    tvStatus.text = "🟢 Live tracking active"
                } else {
                    requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        btnShare.setOnClickListener {
            startActivity(Intent(this, ShareLocationActivity::class.java))
        }
    }

    private fun startTracking() {
        trackingActive = true
        val tvLocation = findViewById<TextView>(R.id.tvTrackingLocation)
        val updateRunnable = object : Runnable {
            override fun run() {
                if (!trackingActive) return
                try {
                    val req = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
                    fusedLocationClient.getCurrentLocation(req, null).addOnSuccessListener { loc ->
                        loc?.let {
                            lastLocation = it
                            tvLocation.text = "📍 Lat: ${String.format("%.5f", it.latitude)}\n🗺 Lon: ${String.format("%.5f", it.longitude)}\n⚡ Accuracy: ${it.accuracy.toInt()}m"
                        }
                    }
                } catch (e: SecurityException) {}
                handler.postDelayed(this, 10_000)
            }
        }
        handler.post(updateRunnable)
    }

    private fun stopTracking() {
        trackingActive = false
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}
