package com.sosence.app

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.sosence.app.data.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class LiveTrackingActivity : BaseActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var webView: WebView
    private lateinit var dbHelper: AppDatabaseHelper

    // UI elements
    private lateinit var llLoadingOverlay: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var viewPulseDot: View
    private lateinit var tvTimer: TextView
    private lateinit var tvGpsSignalPill: TextView
    private lateinit var tvStepStartTime: TextView
    private lateinit var tvStepContactsCount: TextView
    private lateinit var tvLiveAddress: TextView
    private lateinit var tvLiveCoordinates: TextView
    private lateinit var tvLiveSpeed: TextView
    private lateinit var tvLiveDistance: TextView
    private lateinit var tvLiveDuration: TextView
    private lateinit var tvLiveBattery: TextView

    private lateinit var btnBack: ImageView
    private lateinit var btnRecenter: CardView
    private lateinit var btnToggleLayer: CardView
    private lateinit var btnZoomIn: CardView
    private lateinit var btnZoomOut: CardView
    private lateinit var btnShare: CardView
    private lateinit var btnSafeArrival: CardView
    private lateinit var btnPanicSos: CardView

    // Tracking state
    private var isTracking = false
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private var startLocation: Location? = null
    private var totalDistanceMeters: Float = 0f
    private var tripStartTimeMs: Long = 0L
    private var isMapLoaded = false
    private var currentLayerIndex = 0 // 0 = Voyager, 1 = Dark, 2 = OSM

    // Stopwatch Handler
    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTracking && tripStartTimeMs > 0) {
                val elapsedMs = SystemClock.elapsedRealtime() - tripStartTimeMs
                val totalSeconds = (elapsedMs / 1000).toInt()
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                val formattedTime = if (hours > 0) {
                    String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.US, "%02d:%02d", minutes, seconds)
                }

                tvTimer.text = formattedTime
                tvLiveDuration.text = formattedTime
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateBatteryStatus()
        }
    }

    private val locationResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startLiveTracking()
        } else {
            com.sosence.app.utils.LocationHelper.openLocationSettingsDirect(this)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            com.sosence.app.utils.LocationHelper.promptEnableLocation(this, locationResolutionLauncher) {
                startLiveTracking()
            }
        } else {
            Toast.makeText(this, "Location permission is required for live tracking", Toast.LENGTH_LONG).show()
            tvLoadingText.text = "Location permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tracking)
        animateEntrance()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dbHelper = AppDatabaseHelper(this)

        bindViews()
        setupPulseAnimation()
        setupListeners()
        setupLeafletWebView()
        updateBatteryStatus()
        updateEmergencyContactsCount()

        // Register battery monitor
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Auto-start live tracking immediately upon entry
        checkAndStartTracking()
    }

    private fun bindViews() {
        webView = findViewById(R.id.webViewLiveMap)
        llLoadingOverlay = findViewById(R.id.llMapLoadingOverlay)
        tvLoadingText = findViewById(R.id.tvMapLoadingText)
        tvStatusBadge = findViewById(R.id.tvTrackingStatusBadge)
        viewPulseDot = findViewById(R.id.viewLivePulseDot)
        tvTimer = findViewById(R.id.tvTrackingTimer)
        tvGpsSignalPill = findViewById(R.id.tvGpsSignalPill)
        tvStepStartTime = findViewById(R.id.tvStepStartTime)
        tvStepContactsCount = findViewById(R.id.tvStepContactsCount)
        tvLiveAddress = findViewById(R.id.tvLiveAddress)
        tvLiveCoordinates = findViewById(R.id.tvLiveCoordinates)
        tvLiveSpeed = findViewById(R.id.tvLiveSpeed)
        tvLiveDistance = findViewById(R.id.tvLiveDistance)
        tvLiveDuration = findViewById(R.id.tvLiveDuration)
        tvLiveBattery = findViewById(R.id.tvLiveBattery)

        btnBack = findViewById(R.id.ivTrackingBack)
        btnRecenter = findViewById(R.id.btnRecenterMap)
        btnToggleLayer = findViewById(R.id.btnToggleMapLayer)
        btnZoomIn = findViewById(R.id.btnMapZoomIn)
        btnZoomOut = findViewById(R.id.btnMapZoomOut)
        btnShare = findViewById(R.id.btnShareTrackingLocation)
        btnSafeArrival = findViewById(R.id.btnSafeArrival)
        btnPanicSos = findViewById(R.id.btnPanicSos)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        tvStepStartTime.text = "Departed ${timeFormat.format(Date())}"
    }

    private fun setupPulseAnimation() {
        val anim = AlphaAnimation(0.2f, 1.0f).apply {
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        viewPulseDot.startAnimation(anim)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnRecenter.setOnClickListener {
            webView.evaluateJavascript("javascript:recenterMap();", null)
            Toast.makeText(this, "🎯 Focused on current location", Toast.LENGTH_SHORT).show()
        }

        btnToggleLayer.setOnClickListener {
            currentLayerIndex = (currentLayerIndex + 1) % 3
            val layerName = when (currentLayerIndex) {
                0 -> "voyager"
                1 -> "dark"
                else -> "osm"
            }
            webView.evaluateJavascript("javascript:setMapTheme('$layerName');", null)
            val toastMsg = when (currentLayerIndex) {
                0 -> "🗺️ Map theme: Vibrant Street"
                1 -> "🌙 Map theme: Night Dark"
                else -> "🌐 Map theme: OpenStreetMap"
            }
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
        }

        btnZoomIn.setOnClickListener {
            webView.evaluateJavascript("javascript:mapZoomIn();", null)
        }

        btnZoomOut.setOnClickListener {
            webView.evaluateJavascript("javascript:mapZoomOut();", null)
        }

        btnShare.setOnClickListener {
            shareLiveTrackingLink()
        }

        btnSafeArrival.setOnClickListener {
            showSafeArrivalDialog()
        }

        btnPanicSos.setOnClickListener {
            startActivity(Intent(this, SOSCountdownActivity::class.java))
        }
    }

    private fun updateEmergencyContactsCount() {
        val contacts = dbHelper.getAllContacts().filter { it.isEnabled }
        tvStepContactsCount.text = "${contacts.size} Contacts"
    }

    private fun updateBatteryStatus() {
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (batteryLevel >= 0) {
                tvLiveBattery.text = "$batteryLevel%"
                if (batteryLevel <= 15) {
                    tvLiveBattery.setTextColor(ContextCompat.getColor(this, R.color.danger_red))
                } else {
                    tvLiveBattery.setTextColor(ContextCompat.getColor(this, R.color.success_green))
                }
            } else {
                tvLiveBattery.text = "100%"
            }
        } catch (e: Exception) {
            tvLiveBattery.text = "OK"
        }
    }

    private fun setupLeafletWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            databaseEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapLoaded = true
                lastLocation?.let { loc ->
                    pushLocationToMap(loc)
                }
            }
        }

        val htmlContent = getLeafletMapHtml()
        webView.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
    }

    private fun checkAndStartTracking() {
        if (com.sosence.app.utils.LocationHelper.hasLocationPermission(this)) {
            com.sosence.app.utils.LocationHelper.promptEnableLocation(this, locationResolutionLauncher) {
                startLiveTracking()
            }
        } else {
            requestPermissionLauncher.launch(com.sosence.app.utils.LocationHelper.LOCATION_PERMISSIONS)
        }
    }

    private fun startLiveTracking() {
        if (isTracking) return
        isTracking = true
        tripStartTimeMs = SystemClock.elapsedRealtime()
        mainHandler.post(timerRunnable)

        tvStatusBadge.text = "LIVE SHARING"
        tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.success_green))
        llLoadingOverlay.visibility = View.VISIBLE
        tvLoadingText.text = "Connecting to GPS live stream..."

        // High frequency continuous location updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(0.5f)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (isFinishing || isDestroyed) return
                val location = result.lastLocation ?: return
                onNewLocation(location)
            }

            override fun onLocationAvailability(avail: LocationAvailability) {
                if (!avail.isLocationAvailable) {
                    tvGpsSignalPill.text = "⚠️ GPS Signal Weak"
                    tvGpsSignalPill.setTextColor(ContextCompat.getColor(this@LiveTrackingActivity, R.color.warning_yellow))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
            // Also trigger immediate one-shot location to lock fast
            fusedLocationClient.getCurrentLocation(
                CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build(),
                null
            ).addOnSuccessListener { loc ->
                if (!isFinishing && !isDestroyed && loc != null) {
                    onNewLocation(loc)
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location access denied", Toast.LENGTH_SHORT).show()
        }

        // Show ongoing persistent notification
        SOSNotificationManager.showJourneyTrackingNotification(
            this,
            "Active Commute",
            "Real-time GPS trail active • SOSense Guardian"
        )
    }

    private fun onNewLocation(location: Location) {
        llLoadingOverlay.visibility = View.GONE

        if (startLocation == null) {
            startLocation = location
        }

        // Calculate distance delta
        if (lastLocation != null) {
            val delta = lastLocation!!.distanceTo(location)
            // Filter minor GPS drift noise when stationary
            if (delta >= 1.0f && (!location.hasAccuracy() || location.accuracy <= 40f)) {
                totalDistanceMeters += delta
            }
        }
        lastLocation = location

        // Calculate speed (km/h)
        val speedKmh = if (location.hasSpeed()) {
            location.speed * 3.6f
        } else {
            0.0f
        }

        // Update UI metrics
        tvLiveSpeed.text = String.format(Locale.US, "%.1f km/h", speedKmh)
        if (totalDistanceMeters >= 1000f) {
            tvLiveDistance.text = String.format(Locale.US, "%.2f km", totalDistanceMeters / 1000f)
        } else {
            tvLiveDistance.text = String.format(Locale.US, "%.0f m", totalDistanceMeters)
        }

        val accuracyInt = location.accuracy.toInt()
        tvGpsSignalPill.text = "⚡ GPS: ±${accuracyInt}m • High Precision"
        tvGpsSignalPill.setTextColor(ContextCompat.getColor(this, R.color.accent_cyan))

        val latFormatted = String.format(Locale.US, "%.5f", location.latitude)
        val lonFormatted = String.format(Locale.US, "%.5f", location.longitude)
        tvLiveCoordinates.text = "📍 $latFormatted° N, $lonFormatted° E • Updated just now"

        // Push to Leaflet Map
        pushLocationToMap(location)

        // Reverse geocode address in background
        resolveAddressInBackground(location.latitude, location.longitude)
    }

    private fun pushLocationToMap(loc: Location) {
        if (!isMapLoaded) return
        val speed = if (loc.hasSpeed()) loc.speed * 3.6f else 0.0f
        val heading = if (loc.hasBearing()) loc.bearing else -1.0f
        val accuracy = if (loc.hasAccuracy()) loc.accuracy else 10f

        val jsCall = "javascript:updateLivePosition(${loc.latitude}, ${loc.longitude}, $accuracy, $speed, $heading);"
        webView.evaluateJavascript(jsCall, null)
    }

    private fun resolveAddressInBackground(lat: Double, lon: Double) {
        backgroundExecutor.execute {
            try {
                val geocoder = Geocoder(this@LiveTrackingActivity, Locale.getDefault())
                val addresses: List<Address>? = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val feature = addr.featureName
                    val thoroughfare = addr.thoroughfare
                    val subLocality = addr.subLocality
                    val locality = addr.locality

                    val parts = listOfNotNull(
                        if (!thoroughfare.isNullOrBlank()) thoroughfare else feature,
                        subLocality,
                        locality
                    ).filter { it.isNotBlank() }

                    val fullAddress = if (parts.isNotEmpty()) parts.joinToString(", ") else addr.getAddressLine(0) ?: "Active Route Location"

                    mainHandler.post {
                        if (!isFinishing && !isDestroyed) {
                            tvLiveAddress.text = fullAddress
                        }
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    if (!isFinishing && !isDestroyed && tvLiveAddress.text.toString().startsWith("Acquiring")) {
                        tvLiveAddress.text = "Current Commute Trail"
                    }
                }
            }
        }
    }

    private fun shareLiveTrackingLink() {
        val loc = lastLocation
        if (loc == null) {
            Toast.makeText(this, "Acquiring location before sharing...", Toast.LENGTH_SHORT).show()
            return
        }

        val lat = loc.latitude
        val lon = loc.longitude
        val osmLink = "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=17/$lat/$lon"
        val gmapsLink = "https://maps.google.com/?q=$lat,$lon"
        val address = tvLiveAddress.text.toString()
        val duration = tvLiveDuration.text.toString()
        val dist = tvLiveDistance.text.toString()

        val shareMessage = """
            🚨 SOSENSE LIVE JOURNEY BROADCAST 📡
            I am currently commuting and sharing my real-time GPS location trail with you:
            
            📍 Location: $address
            ⚡ Speed: ${tvLiveSpeed.text}
            📏 Traveled: $dist
            ⏱️ Active For: $duration
            
            🗺️ Live OSM Tracker: $osmLink
            🌐 Google Maps: $gmapsLink
            
            Shared securely via SOSense Guardian App.
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "📍 Live Location Tracker Broadcast")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Live Tracking Trail via"))
    }

    private fun showSafeArrivalDialog() {
        val totalTime = tvLiveDuration.text.toString()
        val totalDist = tvLiveDistance.text.toString()
        val currentLoc = tvLiveAddress.text.toString()

        AlertDialog.Builder(this)
            .setTitle("✅ Journey Complete!")
            .setMessage("You have safely ended live tracking.\n\n⏱️ Total Journey Time: $totalTime\n📏 Total Distance Covered: $totalDist\n📍 Destination: $currentLoc\n\nWould you like to send an 'I've Arrived Safely' message to your emergency contacts?")
            .setPositiveButton("Send 'I'm Safe' & Exit") { _, _ ->
                stopLiveTracking()
                sendSafeArrivalNotification()
                finish()
            }
            .setNegativeButton("Just Exit") { _, _ ->
                stopLiveTracking()
                finish()
            }
            .setNeutralButton("Keep Tracking", null)
            .show()
    }

    private fun sendSafeArrivalNotification() {
        val contacts = dbHelper.getAllContacts().filter { it.isEnabled }
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Journey ended. No contacts configured.", Toast.LENGTH_SHORT).show()
            return
        }

        val locStr = tvLiveAddress.text.toString()
        val message = "✅ I have arrived safely at my destination ($locStr). Live tracking session completed. (Sent via SOSense)"

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:" + contacts.joinToString(";") { it.phone })
            putExtra("sms_body", message)
        }
        try {
            startActivity(smsIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "✅ Tracking ended successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLiveTracking() {
        if (!isTracking) return
        isTracking = false
        mainHandler.removeCallbacks(timerRunnable)

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null

        SOSNotificationManager.cancelJourneyTrackingNotification(this)
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onMapReady() {
            mainHandler.post {
                isMapLoaded = true
                lastLocation?.let { pushLocationToMap(it) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLiveTracking()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {}
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (e: Exception) {}
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun getLeafletMapHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    * { box-sizing: border-box; }
                    html, body {
                        margin: 0; padding: 0;
                        width: 100%; height: 100%;
                        background: #161B22;
                        overflow: hidden;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    }
                    #map {
                        width: 100%;
                        height: 100%;
                        background: #161B22;
                    }

                    /* Pulsing Live Rider / User Marker */
                    .pulse-beacon-container {
                        position: relative;
                        width: 50px;
                        height: 50px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .radar-ring {
                        position: absolute;
                        width: 50px;
                        height: 50px;
                        border-radius: 50%;
                        background: rgba(0, 217, 255, 0.4);
                        animation: radarPulse 1.8s cubic-bezier(0.1, 0.8, 0.3, 1) infinite;
                    }
                    .radar-ring-2 {
                        position: absolute;
                        width: 50px;
                        height: 50px;
                        border-radius: 50%;
                        background: rgba(0, 217, 255, 0.25);
                        animation: radarPulse 1.8s cubic-bezier(0.1, 0.8, 0.3, 1) 0.6s infinite;
                    }
                    .beacon-core {
                        position: relative;
                        width: 22px;
                        height: 22px;
                        background: #00D9FF;
                        border: 3.5px solid #FFFFFF;
                        border-radius: 50%;
                        box-shadow: 0 0 16px rgba(0, 217, 255, 1), 0 4px 10px rgba(0,0,0,0.5);
                        z-index: 10;
                    }

                    @keyframes radarPulse {
                        0% { transform: scale(0.3); opacity: 0.9; }
                        100% { transform: scale(1.6); opacity: 0; }
                    }

                    /* Start Origin Flag Marker */
                    .start-flag-marker {
                        background: #06D6A0;
                        color: #0D1117;
                        font-size: 11px;
                        font-weight: 800;
                        padding: 3px 7px;
                        border-radius: 12px;
                        border: 2px solid #FFFFFF;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.4);
                        white-space: nowrap;
                    }

                    /* Custom Leaflet Popup */
                    .leaflet-popup-content-wrapper {
                        background: #16213E;
                        color: #FFFFFF;
                        border-radius: 12px;
                        box-shadow: 0 8px 24px rgba(0,0,0,0.6);
                        border: 1px solid #3A3F5C;
                    }
                    .leaflet-popup-tip {
                        background: #16213E;
                    }
                    .leaflet-popup-content {
                        margin: 10px 14px;
                        font-size: 12px;
                        font-weight: 600;
                        line-height: 1.4;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map;
                    var currentLayer;
                    var userMarker;
                    var accuracyCircle;
                    var breadcrumbPolyline;
                    var breadcrumbGlowPolyline;
                    var startMarker;
                    var coordinatesHistory = [];
                    var lastLat = 13.0827;
                    var lastLon = 80.2707;
                    var userInteracted = false;

                    var tileLayers = {
                        voyager: L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                            maxZoom: 19,
                            attribution: '© OSM © CARTO'
                        }),
                        dark: L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                            maxZoom: 19,
                            attribution: '© OSM © CARTO'
                        }),
                        osm: L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            attribution: '© OpenStreetMap'
                        })
                    };

                    function initMap() {
                        map = L.map('map', {
                            zoomControl: false,
                            attributionControl: false
                        }).setView([lastLat, lastLon], 16);

                        currentLayer = tileLayers.voyager;
                        currentLayer.addTo(map);

                        // Glow background trail
                        breadcrumbGlowPolyline = L.polyline([], {
                            color: '#0077B6',
                            weight: 10,
                            opacity: 0.4,
                            lineCap: 'round',
                            lineJoin: 'round'
                        }).addTo(map);

                        // Main sharp neon trail
                        breadcrumbPolyline = L.polyline([], {
                            color: '#00D9FF',
                            weight: 5,
                            opacity: 0.95,
                            lineCap: 'round',
                            lineJoin: 'round'
                        }).addTo(map);

                        // User drag detection
                        map.on('dragstart', function() {
                            userInteracted = true;
                        });

                        if (window.AndroidBridge && window.AndroidBridge.onMapReady) {
                            window.AndroidBridge.onMapReady();
                        }
                    }

                    function updateLivePosition(lat, lon, accuracy, speed, heading) {
                        lastLat = lat;
                        lastLon = lon;
                        var latlng = [lat, lon];

                        // Add to route trail
                        coordinatesHistory.push(latlng);
                        breadcrumbPolyline.setLatLngs(coordinatesHistory);
                        breadcrumbGlowPolyline.setLatLngs(coordinatesHistory);

                        // Place start marker on first coordinate
                        if (!startMarker && coordinatesHistory.length === 1) {
                            var startIcon = L.divIcon({
                                className: '',
                                html: '<div class="start-flag-marker">🚩 Start</div>',
                                iconSize: [50, 24],
                                iconAnchor: [25, 12]
                            });
                            startMarker = L.marker(latlng, { icon: startIcon }).addTo(map);
                        }

                        // Create or update animated live user marker
                        if (!userMarker) {
                            var pulseIcon = L.divIcon({
                                className: '',
                                html: '<div class="pulse-beacon-container"><div class="radar-ring"></div><div class="radar-ring-2"></div><div class="beacon-core"></div></div>',
                                iconSize: [50, 50],
                                iconAnchor: [25, 25]
                            });
                            userMarker = L.marker(latlng, { icon: pulseIcon, zIndexOffset: 1000 }).addTo(map);
                            userMarker.bindPopup("<b>📍 Live Location</b><br>Broadcasting real-time");
                        } else {
                            userMarker.setLatLng(latlng);
                        }

                        // Accuracy Circle
                        if (!accuracyCircle) {
                            accuracyCircle = L.circle(latlng, {
                                radius: accuracy || 15,
                                color: '#00D9FF',
                                fillColor: '#00D9FF',
                                fillOpacity: 0.12,
                                weight: 1
                            }).addTo(map);
                        } else {
                            accuracyCircle.setLatLng(latlng);
                            accuracyCircle.setRadius(accuracy || 15);
                        }

                        // Smoothly follow user if not dragged manually
                        if (!userInteracted) {
                            map.panTo(latlng, { animate: true, duration: 0.8 });
                        }
                    }

                    function recenterMap() {
                        userInteracted = false;
                        if (userMarker) {
                            map.setView(userMarker.getLatLng(), 17, { animate: true });
                        } else {
                            map.setView([lastLat, lastLon], 17, { animate: true });
                        }
                    }

                    function mapZoomIn() {
                        map.zoomIn();
                    }

                    function mapZoomOut() {
                        map.zoomOut();
                    }

                    function setMapTheme(theme) {
                        if (map && tileLayers[theme]) {
                            map.removeLayer(currentLayer);
                            currentLayer = tileLayers[theme];
                            currentLayer.addTo(map);
                        }
                    }

                    document.addEventListener('DOMContentLoaded', initMap);
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
