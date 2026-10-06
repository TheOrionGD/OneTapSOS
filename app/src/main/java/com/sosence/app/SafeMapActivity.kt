package com.sosence.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SafeMapActivity : BaseActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var webView: WebView
    private lateinit var rvSafeZones: RecyclerView

    private var currentLat = 20.5937
    private var currentLon = 78.9629

    private val TAG = "SafeMapDebug"

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "Permission result: $granted")
        if (granted) {
            loadMapWithCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Showing default location.", Toast.LENGTH_SHORT).show()
            loadDefaultMap()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_map)
        Log.d(TAG, "onCreate called")

        // Screen entrance animation
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        webView = findViewById(R.id.webViewMap)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView.webViewClient = WebViewClient()

        rvSafeZones = findViewById(R.id.rvSafeZones)
        rvSafeZones.layoutManager = LinearLayoutManager(this)

        checkPermissionAndLoadLocation()
    }

    private fun checkPermissionAndLoadLocation() {
        Log.d(TAG, "checkPermissionAndLoadLocation called")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission already granted")
            loadMapWithCurrentLocation()
        } else {
            Log.d(TAG, "Requesting permission")
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun loadMapWithCurrentLocation() {
        Log.d(TAG, "loadMapWithCurrentLocation called")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No permission, loading default")
            loadDefaultMap()
            return
        }

        try {
            val locationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            Log.d(TAG, "Calling getCurrentLocation")
            fusedLocationClient.getCurrentLocation(locationRequest, null)
                .addOnSuccessListener { location: Location? ->
                    Log.d(TAG, "getCurrentLocation SUCCESS, location=$location")
                    if (location != null) {
                        currentLat = location.latitude
                        currentLon = location.longitude
                        loadMapAt(currentLat, currentLon, "You are here")
                        fetchNearbySafeZones(currentLat, currentLon)
                    } else {
                        Log.d(TAG, "location is null, trying lastLocation fallback")
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            Log.d(TAG, "lastLocation fallback result=$lastLoc")
                            if (lastLoc != null) {
                                currentLat = lastLoc.latitude
                                currentLon = lastLoc.longitude
                                loadMapAt(currentLat, currentLon, "You are here (approx)")
                                fetchNearbySafeZones(currentLat, currentLon)
                            } else {
                                Toast.makeText(this, "Could not get precise location. Showing default.", Toast.LENGTH_SHORT).show()
                                loadDefaultMap()
                            }
                        }.addOnFailureListener {
                            Log.e(TAG, "lastLocation fallback failed", it)
                            loadDefaultMap()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "getCurrentLocation FAILED", e)
                    Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                    loadDefaultMap()
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException", e)
            loadDefaultMap()
        }
    }

    private fun focusOnZone(zone: SafeZone) {
        loadMapAt(zone.latitude, zone.longitude, zone.name)
    }

    private fun showDetailsDialog(zone: SafeZone) {
        if (isFinishing || isDestroyed) return
        try {
            val message = "${zone.type}\n\n📍 Address: ${zone.address}\n📞 Phone: ${zone.phone}\n📏 Distance: ${zone.distance}"

            AlertDialog.Builder(this)
                .setTitle(zone.name)
                .setMessage(message)
                .setPositiveButton("Get Directions") { _, _ ->
                    val gmmIntentUri = Uri.parse("google.navigation:q=${zone.latitude},${zone.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
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
        Log.d(TAG, "loadMapAt: $lat, $lon")
        val html = """
            <html>
            <head>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>body { margin:0; padding:0; } #map { height:100vh; width:100vw; }</style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([$lat, $lon], 16);
                    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                        maxZoom: 20,
                        attribution: '© OpenStreetMap contributors © CARTO'
                    }).addTo(map);
                    L.marker([$lat, $lon]).addTo(map).bindPopup('${label.replace("'", "\\'")}').openPopup();
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
    }

    private fun loadDefaultMap() {
        Log.d(TAG, "loadDefaultMap called")
        loadMapAt(currentLat, currentLon, "Default location")
        fetchNearbySafeZones(currentLat, currentLon)
    }

    private fun fetchNearbySafeZones(lat: Double, lon: Double) {
        Log.d(TAG, "fetchNearbySafeZones started: $lat, $lon")
        Thread {
            try {
                val query = """
                    [out:json][timeout:25];
                    (
                      node["amenity"="police"](around:5000,$lat,$lon);
                      node["amenity"="hospital"](around:5000,$lat,$lon);
                      node["amenity"="pharmacy"](around:5000,$lat,$lon);
                      node["amenity"="fire_station"](around:5000,$lat,$lon);
                    );
                    out body;
                """.trimIndent().trim()

                val url = URL("https://overpass-api.de/api/interpreter")
                Log.d(TAG, "Opening Overpass POST connection")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 25000
                connection.readTimeout = 25000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.setRequestProperty("User-Agent", "SOSenseApp/1.0 (safety application)")

                val postData = "data=${URLEncoder.encode(query, "UTF-8")}"
                connection.outputStream.use { it.write(postData.toByteArray()) }

                val responseCode = connection.responseCode
                Log.d(TAG, "Overpass response code: $responseCode")

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                Log.d(TAG, "Overpass response length: ${response.length}")

                val zones = parseOverpassResponse(response.toString(), lat, lon)
                Log.d(TAG, "Parsed zones count: ${zones.size}")

                runOnUiThread {
                    if (zones.isEmpty()) {
                        Toast.makeText(this, "No nearby safe zones found within 5km", Toast.LENGTH_SHORT).show()
                    }
                    rvSafeZones.adapter = SafeZoneAdapter(
                        zones,
                        onZoneClick = { zone -> focusOnZone(zone) },
                        onDetailsClick = { zone -> showDetailsDialog(zone) }
                    )
                    Log.d(TAG, "Adapter set with ${zones.size} items")
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchNearbySafeZones EXCEPTION", e)
                runOnUiThread {
                    Toast.makeText(this, "Could not load nearby places: ${e.message}", Toast.LENGTH_LONG).show()
                    rvSafeZones.adapter = SafeZoneAdapter(emptyList(), {}, {})
                }
            }
        }.start()
    }

    private fun parseOverpassResponse(json: String, userLat: Double, userLon: Double): List<SafeZone> {
        val zones = mutableListOf<SafeZone>()
        val root = org.json.JSONObject(json)
        val elements: JSONArray = root.optJSONArray("elements") ?: JSONArray()

        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags")
            if (tags == null) continue

            val name = tags.optString("name", "")
            if (name.isBlank()) continue

            val amenity = tags.optString("amenity", "")
            val elLat = el.optDouble("lat")
            val elLon = el.optDouble("lon")

            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLon, elLat, elLon, results)
            val distanceKm = results[0] / 1000.0
            val distanceText = String.format("%.1f km", distanceKm)

            val icon = when (amenity) {
                "police" -> "🚔"
                "hospital" -> "🏥"
                "cafe" -> "☕"
                "fire_station" -> "🚒"
                else -> "📍"
            }

            val typeLabel = when (amenity) {
                "police" -> "Police Station"
                "hospital" -> "Hospital"
                "cafe" -> "Cafe"
                "fire_station" -> "Fire Station"
                else -> "Place"
            }

            val addressParts = mutableListOf<String>()
            val houseNumber = tags.optString("addr:housenumber", "")
            if (houseNumber.isNotBlank()) addressParts.add(houseNumber)
            val street = tags.optString("addr:street", "")
            if (street.isNotBlank()) addressParts.add(street)
            val city = tags.optString("addr:city", "")
            if (city.isNotBlank()) addressParts.add(city)
            val address = if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else "Address not available"

            val phone = tags.optString("phone", tags.optString("contact:phone", "Not available"))

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
        }.take(15)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (e: Exception) {}
    }
}

