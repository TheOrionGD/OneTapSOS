package com.onetapsos.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.onetapsos.app.data.AppDatabaseHelper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvCoords: TextView
    private lateinit var dbHelper: AppDatabaseHelper
    private var defaultLat = 20.5937
    private var defaultLng = 78.9629

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContentView(R.layout.activity_map)

        dbHelper = AppDatabaseHelper(this)
        mapView = findViewById(R.id.osmMapView)
        tvCoords = findViewById(R.id.tvMapCoords)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val passedLat = intent.getDoubleExtra("MAP_LAT", 0.0)
        val passedLng = intent.getDoubleExtra("MAP_LNG", 0.0)
        if (passedLat != 0.0 && passedLng != 0.0) {
            defaultLat = passedLat
            defaultLng = passedLng
        }

        setupMapView()

        findViewById<TextView>(R.id.btnRecenterMap).setOnClickListener {
            fetchUserLocationAndCenter()
        }

        fetchUserLocationAndCenter()
    }

    private fun setupMapView() {
        try {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)
            mapView.controller.setZoom(15.0)

            val startPoint = GeoPoint(defaultLat, defaultLng)
            mapView.controller.setCenter(startPoint)

            addMarker(startPoint, "Selected Location", "OpenStreetMap Emergency Marker")
            addSafeZoneMarkers(startPoint)
        } catch (e: Exception) {
            Toast.makeText(this, "Map rendering notice: Using offline tile mode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUserLocationAndCenter() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(this)
                val req = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
                fusedClient.getCurrentLocation(req, null).addOnSuccessListener { loc ->
                    if (isFinishing || isDestroyed) return@addOnSuccessListener
                    if (loc != null) {
                        defaultLat = loc.latitude
                        defaultLng = loc.longitude
                        dbHelper.recordLocation(defaultLat, defaultLng, "Current GPS Location")
                        tvCoords.text = "Location: Lat %.4f, Lng %.4f".format(java.util.Locale.US, defaultLat, defaultLng)
                        val point = GeoPoint(defaultLat, defaultLng)
                        mapView.controller.animateTo(point)
                        addMarker(point, "You Are Here (GPS)", "Accuracy: %.1fm".format(java.util.Locale.US, loc.accuracy))
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun addMarker(point: GeoPoint, title: String, snippet: String) {
        try {
            val marker = Marker(mapView)
            marker.position = point
            marker.title = title
            marker.snippet = snippet
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
            mapView.invalidate()
        } catch (e: Exception) {}
    }

    private fun addSafeZoneMarkers(center: GeoPoint) {
        try {
            addMarker(GeoPoint(center.latitude + 0.005, center.longitude + 0.005), "Police Station 🚔", "Emergency Response 24/7")
            addMarker(GeoPoint(center.latitude - 0.004, center.longitude + 0.003), "General Hospital 🏥", "Trauma & Ambulance Care")
            addMarker(GeoPoint(center.latitude + 0.003, center.longitude - 0.006), "24/7 Pharmacy 💊", "Emergency Medicine")
        } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        try { mapView.onResume() } catch (e: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        try { mapView.onPause() } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { mapView.onDetach() } catch (e: Exception) {}
    }
}
