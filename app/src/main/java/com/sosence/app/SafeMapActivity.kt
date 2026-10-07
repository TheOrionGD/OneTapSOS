package com.sosence.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sosence.app.data.AppDatabaseHelper
import com.sosence.app.data.SafeZoneModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.net.URLEncoder

class SafeMapActivity : BaseActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var webView: WebView
    private lateinit var rvSafeZones: RecyclerView

    private lateinit var pbLoading: ProgressBar
    private lateinit var llError: LinearLayout
    private lateinit var tvErrorMsg: TextView
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnRetry: CardView

    // Category filter chips
    private lateinit var chipAll: TextView
    private lateinit var chipHospitals: TextView
    private lateinit var chipPolice: TextView
    private lateinit var chipPharmacies: TextView
    private lateinit var chipFire: TextView

    private var allZones = mutableListOf<SafeZone>()
    private var displayedZones = mutableListOf<SafeZone>()
    private var selectedCategory: String = "ALL"

    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var isLocationAcquired = false
    private var isLoadingPlaces = false

    private val TAG = "SafeMaps/Map"

    private val overpassEndpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://lz4.overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )

    private val locationResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "[Map] GPS location enabled by user via system dialog")
            loadMapWithRealLocation()
        } else {
            Log.w(TAG, "[Map] User declined to enable GPS location")
            com.sosence.app.utils.LocationHelper.openLocationSettingsDirect(this)
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            Log.d(TAG, "[Map] Location permission granted (fine=$fineGranted, coarse=$coarseGranted)")
            com.sosence.app.utils.LocationHelper.promptEnableLocation(this, locationResolutionLauncher) {
                loadMapWithRealLocation()
            }
        } else {
            Log.w(TAG, "[Map] Location permissions denied by user")
            showLocationError("Location permission is needed to find nearby safe zones.\nPlease grant permission and retry.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_map)
        Log.i(TAG, "[Map] SafeMapActivity onCreate")

        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dbHelper = AppDatabaseHelper(this)

        bindViews()
        setupWebView()
        setupFilterChips()

        checkPermissionAndFetchLocation()
    }

    private fun bindViews() {
        webView = findViewById(R.id.webViewMap)
        rvSafeZones = findViewById(R.id.rvSafeZones)
        rvSafeZones.layoutManager = LinearLayoutManager(this)

        pbLoading = findViewById(R.id.pbSafeMapLoading)
        llError = findViewById(R.id.llSafeMapError)
        tvErrorMsg = findViewById(R.id.tvSafeMapErrorMsg)
        tvCount = findViewById(R.id.tvSafeZonesCount)
        tvEmpty = findViewById(R.id.tvSafeZonesEmpty)
        btnRetry = findViewById(R.id.btnSafeMapRetry)

        chipAll = findViewById(R.id.chipFilterAll)
        chipHospitals = findViewById(R.id.chipFilterHospitals)
        chipPolice = findViewById(R.id.chipFilterPolice)
        chipPharmacies = findViewById(R.id.chipFilterPharmacies)
        chipFire = findViewById(R.id.chipFilterFire)

        findViewById<ImageView>(R.id.ivSafeMapBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.ivSafeMapRefresh).setOnClickListener {
            if (!isLoadingPlaces) {
                checkPermissionAndFetchLocation()
            }
        }

        btnRetry.setOnClickListener {
            llError.visibility = View.GONE
            checkPermissionAndFetchLocation()
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }
        webView.webViewClient = WebViewClient()
    }

    private fun setupFilterChips() {
        val chips = listOf(chipAll, chipHospitals, chipPolice, chipPharmacies, chipFire)

        fun updateChipStyles(activeChip: TextView) {
            chips.forEach { chip ->
                if (chip == activeChip) {
                    chip.setBackgroundResource(R.drawable.bg_badge_granted)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.success_green))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_circle_icon)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
            }
        }

        chipAll.setOnClickListener {
            selectedCategory = "ALL"
            updateChipStyles(chipAll)
            filterAndDisplayZones()
        }

        chipHospitals.setOnClickListener {
            selectedCategory = "Hospital"
            updateChipStyles(chipHospitals)
            filterAndDisplayZones()
        }

        chipPolice.setOnClickListener {
            selectedCategory = "Police Station"
            updateChipStyles(chipPolice)
            filterAndDisplayZones()
        }

        chipPharmacies.setOnClickListener {
            selectedCategory = "Pharmacy"
            updateChipStyles(chipPharmacies)
            filterAndDisplayZones()
        }

        chipFire.setOnClickListener {
            selectedCategory = "Fire Station"
            updateChipStyles(chipFire)
            filterAndDisplayZones()
        }

        // Handle initial filter from intent
        val initialCategory = intent.getStringExtra("FILTER_CATEGORY") ?: intent.getStringExtra("FILTER_TYPE")
        when (initialCategory?.lowercase()) {
            "police" -> {
                selectedCategory = "Police Station"
                updateChipStyles(chipPolice)
            }
            "hospital" -> {
                selectedCategory = "Hospital"
                updateChipStyles(chipHospitals)
            }
            "pharmacy" -> {
                selectedCategory = "Pharmacy"
                updateChipStyles(chipPharmacies)
            }
            "fire", "fire_station" -> {
                selectedCategory = "Fire Station"
                updateChipStyles(chipFire)
            }
            else -> {
                selectedCategory = "ALL"
                updateChipStyles(chipAll)
            }
        }
    }

    private fun checkPermissionAndFetchLocation() {
        if (com.sosence.app.utils.LocationHelper.hasLocationPermission(this)) {
            com.sosence.app.utils.LocationHelper.promptEnableLocation(this, locationResolutionLauncher) {
                loadMapWithRealLocation()
            }
        } else {
            requestLocationPermissionLauncher.launch(com.sosence.app.utils.LocationHelper.LOCATION_PERMISSIONS)
        }
    }

    private fun loadMapWithRealLocation() {
        if (!com.sosence.app.utils.LocationHelper.hasLocationPermission(this)) {
            requestLocationPermissionLauncher.launch(com.sosence.app.utils.LocationHelper.LOCATION_PERMISSIONS)
            return
        }

        pbLoading.visibility = View.VISIBLE
        llError.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        tvCount.text = "Acquiring GPS..."

        try {
            val req = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            Log.i(TAG, "[Map] Requesting current location fix")
            fusedLocationClient.getCurrentLocation(req, null)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        onLocationResolved(loc.latitude, loc.longitude)
                    } else {
                        Log.w(TAG, "[Map] getCurrentLocation returned null, attempting lastLocation fallback")
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                onLocationResolved(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                onLocationFailed("Unable to determine current location.\nPlease ensure GPS is enabled and try again.")
                            }
                        }.addOnFailureListener { e ->
                            onLocationFailed("Location provider error: ${e.localizedMessage}")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "[Map] Location request failed", e)
                    onLocationFailed("Location lookup failed: ${e.localizedMessage}")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "[Map] Location SecurityException", e)
            onLocationFailed("Location permission is missing.")
        }
    }

    private fun onLocationResolved(lat: Double, lon: Double) {
        currentLat = lat
        currentLon = lon
        isLocationAcquired = true
        Log.i(TAG, "[Map] Location resolved: %.4f, %.4f".format(lat, lon))

        loadMapAt(lat, lon, "Your Current Location")
        fetchNearbySafeZones(lat, lon)
    }

    private fun onLocationFailed(reason: String) {
        pbLoading.visibility = View.GONE
        showLocationError(reason)
    }

    private fun showLocationError(msg: String) {
        llError.visibility = View.VISIBLE
        tvErrorMsg.text = msg
        tvCount.text = "GPS Offline"
        rvSafeZones.visibility = View.GONE

        // Check if cached safe zones exist
        val cached = dbHelper.getCachedSafeZones()
        if (cached.isNotEmpty()) {
            Toast.makeText(this, "Showing ${cached.size} cached safe zones (approximate)", Toast.LENGTH_SHORT).show()
            val dummyLat = currentLat ?: 20.5937
            val dummyLon = currentLon ?: 78.9629
            populateFromCache(dummyLat, dummyLon)
        }
    }

    private fun fetchNearbySafeZones(lat: Double, lon: Double) {
        if (isLoadingPlaces) return
        isLoadingPlaces = true
        pbLoading.visibility = View.VISIBLE
        tvCount.text = "Searching safe zones..."
        Log.i(TAG, "[Map] Querying Overpass API endpoints for nearby emergency facilities around ($lat, $lon)")

        Thread {
            val query = """
                [out:json][timeout:15];
                (
                  node["amenity"="police"](around:5000,$lat,$lon);
                  node["amenity"="hospital"](around:5000,$lat,$lon);
                  node["amenity"="pharmacy"](around:5000,$lat,$lon);
                  node["amenity"="fire_station"](around:5000,$lat,$lon);
                );
                out body;
            """.trimIndent().trim()

            var responseJson: String? = null
            var lastError: Exception? = null

            for (endpoint in overpassEndpoints) {
                try {
                    Log.i(TAG, "[Map] Attempting endpoint: $endpoint")
                    val url = URL(endpoint)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 12000
                        readTimeout = 12000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        setRequestProperty("User-Agent", "SOSenseApp/1.0 (safety application)")
                    }

                    val postData = "data=${URLEncoder.encode(query, "UTF-8")}"
                    connection.outputStream.use { it.write(postData.toByteArray()) }

                    val responseCode = connection.responseCode
                    Log.i(TAG, "[Map] Response code: $responseCode from $endpoint")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val sb = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line)
                        }
                        reader.close()
                        responseJson = sb.toString()
                        break // Success!
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[Map] Endpoint $endpoint failed: ${e.message}")
                    lastError = e
                }
            }

            runOnUiThread {
                isLoadingPlaces = false
                pbLoading.visibility = View.GONE

                if (!responseJson.isNullOrBlank()) {
                    try {
                        val zones = parseOverpassResponse(responseJson, lat, lon)
                        Log.i(TAG, "[Map] Parsed ${zones.size} nearby safe zones successfully")

                        allZones.clear()
                        allZones.addAll(zones)

                        // Save into cache for offline resilience
                        dbHelper.saveCachedSafeZones(zones.map {
                            SafeZoneModel(
                                name = it.name,
                                type = it.type,
                                icon = it.icon,
                                distance = it.distance,
                                latitude = it.latitude,
                                longitude = it.longitude,
                                address = it.address,
                                phone = it.phone
                            )
                        })

                        filterAndDisplayZones()
                    } catch (e: Exception) {
                        Log.e(TAG, "[Map] JSON parsing error", e)
                        handleFetchFailure(lat, lon, "Error reading nearby places data.")
                    }
                } else {
                    handleFetchFailure(lat, lon, "Could not load nearby places. Check your internet connection.")
                }
            }
        }.start()
    }

    private fun handleFetchFailure(lat: Double, lon: Double, fallbackMessage: String) {
        val cached = dbHelper.getCachedSafeZones()
        if (cached.isNotEmpty()) {
            Log.i(TAG, "[Map] Network failed, falling back to ${cached.size} cached safe zones")
            Toast.makeText(this, "Network unavailable. Loaded cached safe zones.", Toast.LENGTH_LONG).show()
            populateFromCache(lat, lon)
        } else {
            llError.visibility = View.VISIBLE
            tvErrorMsg.text = fallbackMessage
            tvCount.text = "0 Found"
            rvSafeZones.visibility = View.GONE
        }
    }

    private fun populateFromCache(userLat: Double, userLon: Double) {
        val cached = dbHelper.getCachedSafeZones()
        val zones = cached.map { c ->
            val dist = FloatArray(1)
            Location.distanceBetween(userLat, userLon, c.latitude, c.longitude, dist)
            val km = dist[0] / 1000.0
            SafeZone(
                name = c.name,
                type = c.type,
                icon = c.icon,
                distance = "%.1f km".format(km),
                latitude = c.latitude,
                longitude = c.longitude,
                address = c.address,
                phone = c.phone
            )
        }.sortedBy { it.distance.replace(" km", "").toFloatOrNull() ?: Float.MAX_VALUE }

        allZones.clear()
        allZones.addAll(zones)
        llError.visibility = View.GONE
        filterAndDisplayZones()
    }

    private fun parseOverpassResponse(json: String, userLat: Double, userLon: Double): List<SafeZone> {
        val zones = mutableListOf<SafeZone>()
        val root = JSONObject(json)
        val elements: JSONArray = root.optJSONArray("elements") ?: JSONArray()

        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue

            val name = tags.optString("name", "")
            if (name.isBlank()) continue

            val amenity = tags.optString("amenity", "")
            val elLat = el.optDouble("lat", 0.0)
            val elLon = el.optDouble("lon", 0.0)
            if (elLat == 0.0 || elLon == 0.0) continue

            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLon, elLat, elLon, results)
            val distanceKm = results[0] / 1000.0
            val distanceText = String.format(Locale.getDefault(), "%.1f km", distanceKm)

            val icon = when (amenity) {
                "police" -> "🚔"
                "hospital" -> "🏥"
                "pharmacy" -> "💊"
                "fire_station" -> "🚒"
                else -> "📍"
            }

            val typeLabel = when (amenity) {
                "police" -> "Police Station"
                "hospital" -> "Hospital"
                "pharmacy" -> "Pharmacy"
                "fire_station" -> "Fire Station"
                else -> "Safe Zone"
            }

            val addressParts = mutableListOf<String>()
            val houseNumber = tags.optString("addr:housenumber", "")
            if (houseNumber.isNotBlank()) addressParts.add(houseNumber)
            val street = tags.optString("addr:street", "")
            if (street.isNotBlank()) addressParts.add(street)
            val city = tags.optString("addr:city", "")
            if (city.isNotBlank()) addressParts.add(city)
            val address = if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else "Address not available"

            val phone = tags.optString("phone", tags.optString("contact:phone", "Emergency: 112"))

            zones.add(
                SafeZone(
                    name = name,
                    type = typeLabel,
                    icon = icon,
                    distance = distanceText,
                    latitude = elLat,
                    longitude = elLon,
                    address = address,
                    phone = phone
                )
            )
        }

        return zones.sortedBy {
            it.distance.replace(" km", "").toFloatOrNull() ?: Float.MAX_VALUE
        }
    }

    private fun filterAndDisplayZones() {
        displayedZones = if (selectedCategory == "ALL") {
            allZones.toMutableList()
        } else {
            allZones.filter { it.type.equals(selectedCategory, ignoreCase = true) }.toMutableList()
        }

        tvCount.text = "${displayedZones.size} Found"

        if (displayedZones.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvSafeZones.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvSafeZones.visibility = View.VISIBLE
            rvSafeZones.adapter = SafeZoneAdapter(
                displayedZones,
                onZoneClick = { zone -> focusOnZone(zone) },
                onDetailsClick = { zone -> showDetailsDialog(zone) }
            )
        }
    }

    private fun focusOnZone(zone: SafeZone) {
        loadMapAt(zone.latitude, zone.longitude, "${zone.icon} ${zone.name}")
        Toast.makeText(this, "Focused: ${zone.name}", Toast.LENGTH_SHORT).show()
    }

    private fun showDetailsDialog(zone: SafeZone) {
        if (isFinishing || isDestroyed) return
        try {
            val message = "${zone.type}\n\n📍 Address: ${zone.address}\n📞 Phone: ${zone.phone}\n📏 Distance: ${zone.distance}"

            AlertDialog.Builder(this)
                .setTitle("${zone.icon} ${zone.name}")
                .setMessage(message)
                .setPositiveButton("Get Directions") { _, _ ->
                    val gmmIntentUri = Uri.parse("google.navigation:q=${zone.latitude},${zone.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    try {
                        startActivity(mapIntent)
                    } catch (e: Exception) {
                        val fallbackUri = Uri.parse("https://maps.google.com/?q=${zone.latitude},${zone.longitude}")
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                        } catch (_: Exception) {}
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadMapAt(lat: Double, lon: Double, label: String) {
        Log.d(TAG, "[Map] loadMapAt: $lat, $lon ($label)")
        val escapedLabel = label.replace("'", "\\'").replace("\"", "\\\"")
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body { margin:0; padding:0; background:#161B22; }
                    #map { height:100vh; width:100vw; background:#161B22; }
                    .leaflet-popup-content-wrapper { background:#21262D; color:#F0F6FC; border-radius:8px; }
                    .leaflet-popup-tip { background:#21262D; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map', { zoomControl: false }).setView([$lat, $lon], 15);
                    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                        maxZoom: 19,
                        attribution: '© OSM © CARTO'
                    }).addTo(map);
                    L.marker([$lat, $lon]).addTo(map).bindPopup('$escapedLabel').openPopup();
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (e: Exception) {}
    }
}
