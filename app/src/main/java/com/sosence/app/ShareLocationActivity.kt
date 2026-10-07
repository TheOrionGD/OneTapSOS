package com.sosence.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class ShareLocationActivity : BaseActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var pendingMode: String = "copy"

    private val locationResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) fetchAndShare(pendingMode)
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            com.sosence.app.utils.LocationHelper.promptEnableLocation(this, locationResolutionLauncher) {
                fetchAndShare(pendingMode)
            }
        } else {
            Toast.makeText(this, "Location permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_location)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnBack = findViewById<ImageView>(R.id.ivShareLocBack)
        val btnWhatsApp = findViewById<CardView>(R.id.btnShareWhatsApp)
        val btnSMS = findViewById<CardView>(R.id.btnShareSMS)
        val btnMaps = findViewById<CardView>(R.id.btnShareMaps)
        val btnCopy = findViewById<CardView>(R.id.btnShareCopy)

        btnBack.setOnClickListener { finish() }
        btnWhatsApp.setOnClickListener { checkPermAndShare("whatsapp") }
        btnSMS.setOnClickListener { checkPermAndShare("sms") }
        btnMaps.setOnClickListener { checkPermAndShare("maps") }
        btnCopy.setOnClickListener { checkPermAndShare("copy") }
    }

    private fun checkPermAndShare(mode: String) {
        pendingMode = mode
        if (com.sosence.app.utils.LocationHelper.hasLocationPermission(this)) {
            com.sosence.app.utils.LocationHelper.promptEnableLocation(this, locationResolutionLauncher) {
                fetchAndShare(mode)
            }
        } else {
            requestPermission.launch(com.sosence.app.utils.LocationHelper.LOCATION_PERMISSIONS)
        }
    }

    private fun fetchAndShare(mode: String = "copy") {
        if (isFinishing || isDestroyed) return
        try {
            val req = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
            fusedLocationClient.getCurrentLocation(req, null).addOnSuccessListener { location ->
                if (!isFinishing && !isDestroyed) {
                    shareLocation(location, mode)
                }
            }.addOnFailureListener {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (!isFinishing && !isDestroyed) {
                        shareLocation(loc, mode)
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location unavailable", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareLocation(location: Location?, mode: String) {
        if (location == null) {
            Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
            return
        }
        val lat = location.latitude
        val lon = location.longitude
        val mapsLink = "https://maps.google.com/?q=$lat,$lon"
        val msg = "📍 My current location: $mapsLink\n\nShared via SOSense"

        try {
            when (mode) {
                "whatsapp" -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        setPackage("com.whatsapp")
                        putExtra(Intent.EXTRA_TEXT, msg)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        sharePlain(msg)
                    }
                }
                "sms" -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
                        putExtra("sms_body", msg)
                    }
                    startActivity(intent)
                }
                "maps" -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon"))
                    startActivity(intent)
                }
                "copy" -> {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Location", mapsLink))
                    Toast.makeText(this, "Location link copied!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            sharePlain(msg)
        }
    }

    private fun sharePlain(msg: String) {
        try {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
            }, "Share Location"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not launch share", Toast.LENGTH_SHORT).show()
        }
    }
}

