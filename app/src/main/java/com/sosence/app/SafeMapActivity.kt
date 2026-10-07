package com.sosence.app

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
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
import java.net.URLEncoder
import java.util.Locale

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

    private var currentLat: Double = 13.0827
    private var currentLon: Double = 80.2707
    private var isLocationAcquired = false
    private var isLoadingPlaces = false
    private var isMapLoaded = false

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

    private fun isOnline(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val act = cm.getNetworkCapabilities(net) ?: return false
            act.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = if (isOnline()) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onDirectionsRequested(lat: Double, lon: Double) {
                runOnUiThread {
                    openGoogleMapsDirections(lat, lon)
                }
            }

            @JavascriptInterface
            fun onMapLoaded() {
                runOnUiThread {
                    isMapLoaded = true
                    updateMapMarkers()
                }
            }
        }, "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapLoaded = true
                updateMapMarkers()
            }
        }

        loadGoogleMapsHtml(currentLat, currentLon)
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

        // Center map to user position
        updateUserLocationOnMap(lat, lon)
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
            Toast.makeText(this, "Showing ${cached.size} cached safe zones (offline)", Toast.LENGTH_SHORT).show()
            populateFromCache(currentLat, currentLon)
        } else {
            populateDefaultEmergencyHubs(currentLat, currentLon)
        }
    }

    private fun fetchNearbySafeZones(lat: Double, lon: Double) {
        if (!isOnline()) {
            Log.i(TAG, "[Map] Device is offline, loading cached safe zones immediately")
            handleFetchFailure(lat, lon, "Offline mode • Showing saved emergency zones.")
            return
        }

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

            for (endpoint in overpassEndpoints) {
                try {
                    Log.i(TAG, "[Map] Attempting endpoint: $endpoint")
                    val url = URL(endpoint)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 10000
                        readTimeout = 10000
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
                }
            }

            runOnUiThread {
                isLoadingPlaces = false
                pbLoading.visibility = View.GONE

                if (!responseJson.isNullOrBlank()) {
                    try {
                        val zones = parseOverpassResponse(responseJson, lat, lon)
                        Log.i(TAG, "[Map] Parsed ${zones.size} nearby safe zones successfully")

                        if (zones.isNotEmpty()) {
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
                        } else {
                            handleFetchFailure(lat, lon, "No emergency services found nearby.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[Map] JSON parsing error", e)
                        handleFetchFailure(lat, lon, "Error reading nearby places data.")
                    }
                } else {
                    handleFetchFailure(lat, lon, "Offline mode • Showing saved emergency zones.")
                }
            }
        }.start()
    }

    private fun handleFetchFailure(lat: Double, lon: Double, fallbackMessage: String) {
        val cached = dbHelper.getCachedSafeZones()
        if (cached.isNotEmpty()) {
            Log.i(TAG, "[Map] Network failed, falling back to ${cached.size} cached safe zones")
            Toast.makeText(this, "Offline mode: loaded ${cached.size} cached safe zones.", Toast.LENGTH_SHORT).show()
            populateFromCache(lat, lon)
        } else {
            populateDefaultEmergencyHubs(lat, lon)
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

    private fun populateDefaultEmergencyHubs(userLat: Double, userLon: Double) {
        val defaults = listOf(
            SafeZone("National Emergency Helpline", "Police Station", "🚔", "0.0 km", userLat + 0.005, userLon + 0.005, "Toll-Free 24/7 Response", "112"),
            SafeZone("Police Emergency Services", "Police Station", "🚔", "0.5 km", userLat - 0.004, userLon + 0.006, "Police Control Room", "100"),
            SafeZone("Emergency Medical & Ambulance", "Hospital", "🏥", "0.8 km", userLat + 0.007, userLon - 0.005, "Ambulance Dispatch Hub", "108"),
            SafeZone("Fire & Rescue Headquarters", "Fire Station", "🚒", "1.2 km", userLat - 0.008, userLon - 0.006, "Fire Emergency Operations", "101"),
            SafeZone("Emergency Pharmacy Services", "Pharmacy", "💊", "1.5 km", userLat + 0.003, userLon + 0.008, "24-Hour Emergency Medical Store", "104")
        )

        allZones.clear()
        allZones.addAll(defaults)
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
            val address = if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else "Emergency Zone, Local Area"

            val phone = tags.optString("phone", tags.optString("contact:phone", "112"))

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

        tvCount.text = "${displayedZones.size} Safe Zones"

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

        updateMapMarkers()
    }

    private fun focusOnZone(zone: SafeZone) {
        val escapedName = zone.name.replace("'", "\\'").replace("\"", "\\\"")
        val js = "if(window.focusZone) { window.focusZone(${zone.latitude}, ${zone.longitude}, '$escapedName'); }"
        webView.evaluateJavascript(js, null)
        Toast.makeText(this, "Focused: ${zone.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateUserLocationOnMap(lat: Double, lon: Double) {
        val js = "if(window.setUserLocation) { window.setUserLocation($lat, $lon); }"
        webView.evaluateJavascript(js, null)
    }

    private fun updateMapMarkers() {
        if (!isMapLoaded) return
        val jsonArray = JSONArray()
        for (z in displayedZones) {
            val obj = JSONObject()
            obj.put("name", z.name)
            obj.put("type", z.type)
            obj.put("icon", z.icon)
            obj.put("lat", z.latitude)
            obj.put("lon", z.longitude)
            obj.put("distance", z.distance)
            obj.put("address", z.address)
            obj.put("phone", z.phone)
            jsonArray.put(obj)
        }

        val jsonStr = jsonArray.toString()
        val js = "if(window.renderSafeZones) { window.renderSafeZones($jsonStr); }"
        webView.evaluateJavascript(js, null)
    }

    private fun openGoogleMapsDirections(lat: Double, lon: Double) {
        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            val fallbackUri = Uri.parse("https://maps.google.com/?q=$lat,$lon")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
            } catch (_: Exception) {}
        }
    }

    private fun showDetailsDialog(zone: SafeZone) {
        if (isFinishing || isDestroyed) return
        try {
            val message = "${zone.type}\n\n📍 Address: ${zone.address}\n📞 Phone: ${zone.phone}\n📏 Distance: ${zone.distance}"

            AlertDialog.Builder(this)
                .setTitle("${zone.icon} ${zone.name}")
                .setMessage(message)
                .setPositiveButton("Get Directions") { _, _ ->
                    openGoogleMapsDirections(zone.latitude, zone.longitude)
                }
                .setNeutralButton("Call") { _, _ ->
                    val cleanPhone = zone.phone.replace("[^0-9+]".toRegex(), "")
                    if (cleanPhone.isNotBlank()) {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                        startActivity(callIntent)
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadGoogleMapsHtml(lat: Double, lon: Double) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    * { box-sizing: border-box; margin:0; padding:0; }
                    html, body { width:100%; height:100%; background:#161B22; overflow:hidden; font-family:-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
                    #map { width:100%; height:100%; background:#161B22; }

                    /* Google Maps style popup card */
                    .leaflet-popup-content-wrapper {
                        background: #1F2937;
                        color: #F9FAFB;
                        border-radius: 12px;
                        box-shadow: 0 8px 24px rgba(0,0,0,0.5);
                        padding: 0;
                        overflow: hidden;
                        border: 1px solid #374151;
                    }
                    .leaflet-popup-content {
                        margin: 10px 14px;
                        line-height: 1.4;
                    }
                    .leaflet-popup-tip {
                        background: #1F2937;
                    }
                    .popup-title {
                        font-weight: 700;
                        font-size: 14px;
                        color: #FFFFFF;
                        margin-bottom: 2px;
                    }
                    .popup-type {
                        font-size: 11px;
                        font-weight: 600;
                        color: #00D9FF;
                        margin-bottom: 6px;
                    }
                    .popup-dist {
                        font-size: 11px;
                        color: #9CA3AF;
                        margin-bottom: 8px;
                    }
                    .popup-btn {
                        display: inline-block;
                        background: #00D9FF;
                        color: #0D1117;
                        font-size: 11px;
                        font-weight: bold;
                        padding: 4px 10px;
                        border-radius: 6px;
                        text-decoration: none;
                        cursor: pointer;
                    }

                    /* Pulsing User Location Beacon */
                    .gmap-user-beacon {
                        position: relative;
                        width: 40px;
                        height: 40px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .gmap-user-pulse {
                        position: absolute;
                        width: 36px;
                        height: 36px;
                        border-radius: 50%;
                        background: rgba(66, 133, 244, 0.4);
                        animation: gmapPulse 2s cubic-bezier(0.2, 0.8, 0.2, 1) infinite;
                    }
                    .gmap-user-dot {
                        position: relative;
                        width: 16px;
                        height: 16px;
                        background: #4285F4;
                        border: 3px solid #FFFFFF;
                        border-radius: 50%;
                        box-shadow: 0 0 8px rgba(66, 133, 244, 0.8), 0 2px 6px rgba(0,0,0,0.4);
                        z-index: 10;
                    }
                    @keyframes gmapPulse {
                        0% { transform: scale(0.4); opacity: 0.9; }
                        100% { transform: scale(1.8); opacity: 0; }
                    }

                    /* Safe Zone Pin Marker */
                    .zone-pin-wrap {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        cursor: pointer;
                        filter: drop-shadow(0 3px 6px rgba(0,0,0,0.5));
                    }
                    .zone-pin-badge {
                        background: #1F2937;
                        border: 2px solid #00D9FF;
                        border-radius: 20px;
                        padding: 2px 6px;
                        font-size: 13px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .zone-pin-tip {
                        width: 0;
                        height: 0;
                        border-left: 5px solid transparent;
                        border-right: 5px solid transparent;
                        border-top: 6px solid #00D9FF;
                        margin-top: -1px;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map;
                    var userMarker;
                    var userAccuracyCircle;
                    var zoneMarkersLayer;
                    var currentLat = $lat;
                    var currentLon = $lon;

                    // Authentic Google Maps standard tile layer (0 watermark, 0 API key required)
                    var gmapStandard = L.tileLayer('https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
                        maxZoom: 20,
                        subdomains: ['0', '1', '2', '3'],
                        attribution: '© Google Maps'
                    });

                    var gmapSatellite = L.tileLayer('https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
                        maxZoom: 20,
                        subdomains: ['0', '1', '2', '3'],
                        attribution: '© Google Maps'
                    });

                    var gmapTerrain = L.tileLayer('https://mt{s}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}', {
                        maxZoom: 20,
                        subdomains: ['0', '1', '2', '3'],
                        attribution: '© Google Maps'
                    });

                    var osmStandard = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap'
                    });

                    map = L.map('map', {
                        zoomControl: true,
                        attributionControl: false,
                        layers: [gmapStandard]
                    }).setView([currentLat, currentLon], 15);

                    var baseMaps = {
                        "Google Map": gmapStandard,
                        "Satellite": gmapSatellite,
                        "Terrain": gmapTerrain,
                        "OpenStreetMap": osmStandard
                    };
                    L.control.layers(baseMaps, null, { position: 'topright' }).addTo(map);

                    zoneMarkersLayer = L.layerGroup().addTo(map);

                    // User Location
                    window.setUserLocation = function(lat, lon) {
                        currentLat = lat;
                        currentLon = lon;
                        var latlng = [lat, lon];
                        if (!userMarker) {
                            var beaconIcon = L.divIcon({
                                className: '',
                                html: '<div class="gmap-user-beacon"><div class="gmap-user-pulse"></div><div class="gmap-user-dot"></div></div>',
                                iconSize: [40, 40],
                                iconAnchor: [20, 20]
                            });
                            userMarker = L.marker(latlng, { icon: beaconIcon, zIndexOffset: 1000 }).addTo(map);
                            userMarker.bindPopup('<div class="popup-title">📍 Your Location</div><div class="popup-type">GPS Live Position</div>');
                        } else {
                            userMarker.setLatLng(latlng);
                        }
                        map.panTo(latlng, { animate: true });
                    };

                    // Render Safe Zones
                    window.renderSafeZones = function(zones) {
                        zoneMarkersLayer.clearLayers();
                        if (!zones || !zones.length) return;

                        zones.forEach(function(zone) {
                            var borderColor = '#00D9FF';
                            if (zone.type === 'Hospital') borderColor = '#EF4444';
                            else if (zone.type === 'Police Station') borderColor = '#3B82F6';
                            else if (zone.type === 'Fire Station') borderColor = '#F59E0B';
                            else if (zone.type === 'Pharmacy') borderColor = '#10B981';

                            var pinHtml = '<div class="zone-pin-wrap">' +
                                '<div class="zone-pin-badge" style="border-color:' + borderColor + '">' + (zone.icon || '📍') + '</div>' +
                                '<div class="zone-pin-tip" style="border-top-color:' + borderColor + '"></div>' +
                                '</div>';

                            var pinIcon = L.divIcon({
                                className: '',
                                html: pinHtml,
                                iconSize: [32, 36],
                                iconAnchor: [16, 36],
                                popupAnchor: [0, -34]
                            });

                            var marker = L.marker([zone.lat, zone.lon], { icon: pinIcon });
                            var popupContent = '<div class="popup-title">' + (zone.icon || '') + ' ' + zone.name + '</div>' +
                                '<div class="popup-type" style="color:' + borderColor + '">' + zone.type + '</div>' +
                                '<div class="popup-dist">📏 ' + zone.distance + ' • ' + (zone.address || '') + '</div>' +
                                '<a class="popup-btn" onclick="AndroidBridge.onDirectionsRequested(' + zone.lat + ',' + zone.lon + ')">🗺️ Directions</a>';

                            marker.bindPopup(popupContent);
                            zoneMarkersLayer.addLayer(marker);
                        });
                    };

                    // Focus on zone from list
                    window.focusZone = function(lat, lon, name) {
                        map.flyTo([lat, lon], 17, { animate: true, duration: 1.0 });
                        zoneMarkersLayer.eachLayer(function(marker) {
                            var mPos = marker.getLatLng();
                            if (Math.abs(mPos.lat - lat) < 0.0001 && Math.abs(mPos.lng - lon) < 0.0001) {
                                marker.openPopup();
                            }
                        });
                    };

                    // Notify Android
                    if (window.AndroidBridge && window.AndroidBridge.onMapLoaded) {
                        window.AndroidBridge.onMapLoaded();
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://google.com", html, "text/html", "UTF-8", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (e: Exception) {}
    }
}

