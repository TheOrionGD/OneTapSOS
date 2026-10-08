package com.onetapsos.app

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onetapsos.app.adapters.SafetyEventAdapter
import com.onetapsos.app.data.AppDatabaseHelper
import com.onetapsos.app.engine.BackgroundSafetyEngine
import com.onetapsos.app.engine.SafetyEvent

class BackgroundDiagnosticsActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var eventAdapter: SafetyEventAdapter
    private lateinit var rvEvents: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background_diagnostics)

        dbHelper = AppDatabaseHelper(this)

        findViewById<ImageView>(R.id.ivDiagBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.ivRefreshDiag).setOnClickListener {
            refreshDiagnostics()
            Toast.makeText(this, "Diagnostics refreshed", Toast.LENGTH_SHORT).show()
        }

        // Initialize RecyclerView
        rvEvents = findViewById(R.id.rvSafetyEvents)
        tvEmpty = findViewById(R.id.tvEmptyEvents)
        rvEvents.layoutManager = LinearLayoutManager(this)
        eventAdapter = SafetyEventAdapter(emptyList())
        rvEvents.adapter = eventAdapter

        // Clear button
        findViewById<TextView>(R.id.btnClearEvents).setOnClickListener {
            dbHelper.clearSafetyEvents()
            loadSafetyEvents()
            Toast.makeText(this, "Event history cleared", Toast.LENGTH_SHORT).show()
        }

        // Subsystem readiness row click listeners for direct permission config
        findViewById<View>(R.id.rowDiagBattery).setOnClickListener {
            startActivity(Intent(this, BatteryAlertSettingsActivity::class.java))
        }

        findViewById<View>(R.id.rowDiagAlarm).setOnClickListener {
            com.onetapsos.app.engine.BackgroundPermissionHelper.checkExactAlarmWithRationale(this) {
                refreshDiagnostics()
            }
        }

        findViewById<View>(R.id.rowDiagNotif).setOnClickListener {
            com.onetapsos.app.engine.BackgroundPermissionHelper.checkNotificationWithRationale(this) {
                refreshDiagnostics()
            }
        }

        findViewById<View>(R.id.rowDiagOpt).setOnClickListener {
            com.onetapsos.app.engine.BackgroundPermissionHelper.checkBatteryOptimizationWithRationale(this) {
                refreshDiagnostics()
            }
        }

        findViewById<View>(R.id.rowDiagFall).setOnClickListener {
            val isRunning = FallDetectionService.isServiceRunning || appSettings.isFallDetectionEnabled
            if (isRunning) {
                appSettings.isFallDetectionEnabled = false
                val stopIntent = Intent(this, FallDetectionService::class.java).apply {
                    action = FallDetectionService.ACTION_STOP
                }
                startService(stopIntent)
                Toast.makeText(this, "Fall Detection Stopped", Toast.LENGTH_SHORT).show()
            } else {
                appSettings.isFallDetectionEnabled = true
                val startIntent = Intent(this, FallDetectionService::class.java).apply {
                    action = FallDetectionService.ACTION_START
                }
                androidx.core.content.ContextCompat.startForegroundService(this, startIntent)
                Toast.makeText(this, "Fall Detection Started", Toast.LENGTH_SHORT).show()
            }
            refreshDiagnostics()
        }

        setupTestButtons()
        refreshDiagnostics()
        loadSafetyEvents()
    }

    private fun setupTestButtons() {
        // 0. Simulate Fall Event
        findViewById<View>(R.id.btnSimulateFallDetection).setOnClickListener {
            FallDetectionService.simulateFall(this)
            Toast.makeText(this, "Simulated Fall Event Triggered! (15s Confirmation Window)", Toast.LENGTH_LONG).show()
            window.decorView.postDelayed({ loadSafetyEvents() }, 1000)
        }

        // 1. Simulate Low Battery
        findViewById<View>(R.id.btnSimulateLowBattery).setOnClickListener {
            val threshold = appSettings.lowBatteryThreshold
            BackgroundSafetyEngine.dispatchEvent(
                this,
                SafetyEvent.LowBattery(percentage = threshold, isCharging = false, health = "Good")
            )
            Toast.makeText(this, "Simulated $threshold% low battery event dispatched!", Toast.LENGTH_SHORT).show()
            loadSafetyEvents()
        }

        // 2. Test 10s Exact Alarm Safety Timer
        findViewById<View>(R.id.btnTestBackgroundTimer).setOnClickListener {
            BackgroundSafetyEngine.scheduleSafetyTimer(this, 1, "Diagnostic Test (1m)")
            Toast.makeText(this, "1-minute exact safety timer armed! You may lock the device now.", Toast.LENGTH_LONG).show()
            loadSafetyEvents()
        }

        // 3. Reset Event Latch
        findViewById<View>(R.id.btnResetEventLatch).setOnClickListener {
            appSettings.isLowBatteryStateActive = false
            appSettings.lastLowBatteryTriggerTimestamp = 0L
            Toast.makeText(this, "Low battery state latch & cooldown reset.", Toast.LENGTH_SHORT).show()
            refreshDiagnostics()
        }
    }

    private fun refreshDiagnostics() {
        // 1. Battery Receiver
        val tvBattery = findViewById<TextView>(R.id.tvDiagBatteryStatus)
        val threshold = appSettings.lowBatteryThreshold
        val isEnabled = appSettings.isBatteryAlertEnabled && appSettings.isBackgroundSafetyEnabled
        tvBattery.text = if (isEnabled) "✓ Active ($threshold% Threshold)" else "✕ Disabled (Tap to set)"
        tvBattery.setTextColor(getColor(if (isEnabled) R.color.success_green else R.color.danger_red))

        // 2. Alarm Scheduler
        val tvAlarm = findViewById<TextView>(R.id.tvDiagAlarmStatus)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
        tvAlarm.text = if (canScheduleExact) "✓ Ready (Exact Alarms Granted)" else "⚠️ Limited (Tap to Grant)"
        tvAlarm.setTextColor(getColor(if (canScheduleExact) R.color.success_green else R.color.warning_yellow))

        // 3. Boot Status
        findViewById<TextView>(R.id.tvDiagBootStatus).apply {
            text = "✓ Configured in Manifest"
            setTextColor(getColor(R.color.success_green))
        }

        // 4. Notification Channels & Permissions
        val tvNotif = findViewById<TextView>(R.id.tvDiagNotifStatus)
        val notifGranted = com.onetapsos.app.engine.BackgroundPermissionHelper.isNotificationPermissionGranted(this)
        tvNotif.text = if (notifGranted) "✓ Active & Granted" else "⚠️ Denied (Tap to Grant)"
        tvNotif.setTextColor(getColor(if (notifGranted) R.color.success_green else R.color.warning_yellow))

        // 5. Battery Optimization Status
        val tvOpt = findViewById<TextView>(R.id.tvDiagOptStatus)
        val isIgnoringOptimizations = com.onetapsos.app.engine.BackgroundPermissionHelper.isBatteryOptimizationExempt(this)
        tvOpt.text = if (isIgnoringOptimizations) "Exempt / Unrestricted" else "Standard (Tap to Optimize)"
        tvOpt.setTextColor(getColor(if (isIgnoringOptimizations) R.color.success_green else R.color.accent_cyan))

        // 6. Fall Detection Sensor Subsystem
        val tvFall = findViewById<TextView>(R.id.tvDiagFallStatus)
        val isFallRunning = FallDetectionService.isServiceRunning || appSettings.isFallDetectionEnabled
        val isAccAvailable = FallDetectionService.isAccelerometerAvailable
        if (isFallRunning) {
            tvFall.text = "✓ Monitoring Active"
            tvFall.setTextColor(getColor(R.color.success_green))
        } else if (!isAccAvailable) {
            tvFall.text = "✕ Sensor Unavailable"
            tvFall.setTextColor(getColor(R.color.danger_red))
        } else {
            tvFall.text = "✕ Disabled (Tap to Enable)"
            tvFall.setTextColor(getColor(R.color.text_muted))
        }
    }

    private fun loadSafetyEvents() {
        val events = dbHelper.getAllSafetyEvents(50)
        eventAdapter.updateData(events)
        tvEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        rvEvents.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        refreshDiagnostics()
        loadSafetyEvents()
    }
}
