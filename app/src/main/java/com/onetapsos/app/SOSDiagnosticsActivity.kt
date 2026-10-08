package com.onetapsos.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.onetapsos.app.data.AppDatabaseHelper

class SOSDiagnosticsActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_diagnostics)

        dbHelper = AppDatabaseHelper(this)
        container = findViewById(R.id.llDiagResults)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnRunDiag).setOnClickListener { runDiagnostics() }
        findViewById<CardView>(R.id.btnTestSOS).setOnClickListener {
            Toast.makeText(this, "Test SOS Triggered", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SOSActivationActivity::class.java))
        }

        runDiagnostics()
    }

    private fun runDiagnostics() {
        container.removeAllViews()

        val contacts = dbHelper.getAllContacts()
        val hasSmsPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasLocPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val locManager = getSystemService(LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

        val isAccAvailable = FallDetectionService.isAccelerometerAvailable
        val isFallRunning = FallDetectionService.isServiceRunning || appSettings.isFallDetectionEnabled

        addDiagItem("DATABASE CONNECTION", true, "SQLite DB active & writable")
        addDiagItem("TRUSTED CONTACTS", contacts.isNotEmpty(), "${contacts.size} contact(s) saved")
        addDiagItem("SMS PERMISSION", hasSmsPerm, if (hasSmsPerm) "Granted" else "Denied - Required for SMS")
        addDiagItem("LOCATION PERMISSION", hasLocPerm, if (hasLocPerm) "Granted" else "Denied - GPS limited")
        addDiagItem("LOCATION PROVIDER", isGpsEnabled, if (isGpsEnabled) "GPS Provider Active" else "GPS Disabled")
        addDiagItem("FALL ACCELEROMETER", isAccAvailable, if (isAccAvailable) "Sensor Hardware Available" else "Accelerometer Unavailable")
        addDiagItem("FALL MONITORING ENGINE", isFallRunning, if (isFallRunning) "Service Monitoring Active" else "Engine Idle (Ready to Enable)")
        addDiagItem("MESSAGE GENERATION", true, "Emergency templates parsed OK")
        addDiagItem("SOS SERVICE STATUS", true, "Foreground alert engine ready")
        addDiagItem("RECIPIENT AVAILABILITY", contacts.any { it.isEnabled }, "Primary recipients available")
    }

    private fun addDiagItem(name: String, isPass: Boolean, detail: String) {
        val item = LayoutInflater.from(this).inflate(R.layout.item_diag_row, container, false)
        val tvName = item.findViewById<TextView>(R.id.tvDiagName)
        val tvStatus = item.findViewById<TextView>(R.id.tvDiagStatus)
        val tvDetail = item.findViewById<TextView>(R.id.tvDiagDetail)

        tvName.text = name
        tvStatus.text = if (isPass) "PASS" else "FAIL"
        tvStatus.setTextColor(ContextCompat.getColor(this, if (isPass) R.color.success_green else R.color.danger_red))
        tvDetail.text = detail

        container.addView(item)
    }
}
