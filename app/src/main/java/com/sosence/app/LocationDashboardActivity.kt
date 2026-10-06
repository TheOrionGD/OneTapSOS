package com.sosence.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sosence.app.data.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationDashboardActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_dashboard)

        dbHelper = AppDatabaseHelper(this)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val tvGpsStatus = findViewById<TextView>(R.id.tvGpsStatus)
        val tvCoords = findViewById<TextView>(R.id.tvLocationCoords)
        val tvAccuracy = findViewById<TextView>(R.id.tvAccuracyValue)
        val tvLastUpdated = findViewById<TextView>(R.id.tvLastUpdatedTime)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            tvGpsStatus.text = "Active (High Accuracy GPS)"
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(this)
                val req = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
                fusedClient.getCurrentLocation(req, null).addOnSuccessListener { loc ->
                    if (loc != null) {
                        tvCoords.text = "%.4f, %.4f".format(loc.latitude, loc.longitude)
                        tvAccuracy.text = "± %.1f meters".format(loc.accuracy)
                        tvLastUpdated.text = SimpleDateFormat("HH:mm:ss, dd MMM", Locale.getDefault()).format(Date(loc.time))
                        dbHelper.recordLocation(loc.latitude, loc.longitude, "Location Dashboard GPS Update")
                    } else {
                        tvCoords.text = "Location Signal Searching..."
                    }
                }
            } catch (e: Exception) {}
        } else {
            tvGpsStatus.text = "Location Permission Denied"
            tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.danger_red))
        }

        findViewById<CardView>(R.id.btnOpenFullMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        findViewById<CardView>(R.id.btnViewLocationHistory).setOnClickListener {
            startActivity(Intent(this, LocationHistoryActivity::class.java))
        }
    }
}
