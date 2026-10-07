package com.sosence.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sosence.app.data.AppDatabaseHelper
import com.sosence.app.data.SafetyEventRecord
import com.sosence.app.engine.BackgroundSafetyEngine
import com.sosence.app.engine.SafetyEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

enum class FallDetectionState {
    IDLE,
    MONITORING_NORMAL,
    FREE_FALL_CANDIDATE,
    IMPACT_DETECTED,
    POST_IMPACT_ANALYSIS,
    FALL_CONFIRMED_COUNTDOWN,
    TRIGGERING_SOS
}

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var gravitySensor: Sensor? = null
    private var linearAccelerationSensor: Sensor? = null

    private lateinit var appSettings: AppSettings
    private val mainHandler = Handler(Looper.getMainLooper())

    private var confirmationTimer: CountDownTimer? = null
    private val channelId = "sosense_fall_detection"
    private val quietNotificationId = 2000
    private val alertNotificationId = 2001

    // State machine variables
    private var currentState = FallDetectionState.IDLE
    private var freeFallStartTime = 0L
    private var impactTime = 0L
    private val postImpactMagnitudes = mutableListOf<Float>()
    private var postImpactAnalysisRunnable: Runnable? = null

    companion object {
        private const val TAG = "SafeMaps/Fall"
        const val ACTION_START = "com.sosence.app.ACTION_START_FALL_MONITORING"
        const val ACTION_STOP = "com.sosence.app.ACTION_STOP_FALL_MONITORING"
        const val ACTION_CANCEL_FALL = "com.sosence.app.ACTION_CANCEL_FALL"
        const val ACTION_SIMULATE_FALL = "com.sosence.app.ACTION_SIMULATE_FALL"
        const val ACTION_TRIGGER_INSTANT_SOS = "com.sosence.app.ACTION_TRIGGER_INSTANT_SOS"

        // Diagnostics / Real-time Telemetry
        var isServiceRunning: Boolean = false
            private set
        var isAccelerometerAvailable: Boolean = false
            private set
        var isGyroscopeAvailable: Boolean = false
            private set
        var isGravityAvailable: Boolean = false
            private set
        var lastSensorTimestamp: Long = 0L
            private set
        var currentAccX: Float = 0f
            private set
        var currentAccY: Float = 0f
            private set
        var currentAccZ: Float = 0f
            private set
        var currentMagnitude: Float = 9.8f
            private set
        var currentDetectionState: FallDetectionState = FallDetectionState.IDLE
            private set
        var lastCandidateTimestamp: Long = 0L
            private set
        var lastConfirmedFallTimestamp: Long = 0L
            private set

        fun simulateFall(context: Context) {
            val intent = Intent(context, FallDetectionService::class.java).apply {
                action = ACTION_SIMULATE_FALL
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "[Fall] Service onCreate")
        appSettings = AppSettings(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        isAccelerometerAvailable = (accelerometer != null)
        isGyroscopeAvailable = (gyroscope != null)
        isGravityAvailable = (gravitySensor != null)

        Log.i(TAG, "[Fall] Sensors available -> Acc: $isAccelerometerAvailable, Gyro: $isGyroscopeAvailable, Gravity: $isGravityAvailable")

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "[Fall] onStartCommand with action: $action")

        when (action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CANCEL_FALL -> {
                cancelFallAlert()
                return START_STICKY
            }
            ACTION_SIMULATE_FALL -> {
                startMonitoringForeground()
                simulateFallEvent()
                return START_STICKY
            }
            ACTION_TRIGGER_INSTANT_SOS -> {
                confirmationTimer?.cancel()
                triggerFallSOS()
                return START_STICKY
            }
            else -> {
                startMonitoringForeground()
                return START_STICKY
            }
        }
    }

    private fun startMonitoringForeground() {
        try {
            val notif = buildQuietNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    quietNotificationId,
                    notif,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(quietNotificationId, notif)
            }
            isServiceRunning = true
            appSettings.isFallDetectionEnabled = true
            setDetectionState(FallDetectionState.MONITORING_NORMAL)

            registerSensors()
            Log.i(TAG, "[Fall] Sensor monitoring started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "[Fall] Failed to start foreground monitoring: ${e.message}", e)
        }
    }

    private fun registerSensors() {
        try {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Fall] Failed to register sensors: ${e.message}", e)
        }
    }

    private fun unregisterSensors() {
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "[Fall] Failed to unregister sensors: ${e.message}", e)
        }
    }

    private fun stopMonitoring() {
        Log.i(TAG, "[Fall] Stopping fall monitoring service")
        confirmationTimer?.cancel()
        postImpactAnalysisRunnable?.let { mainHandler.removeCallbacks(it) }
        unregisterSensors()
        isServiceRunning = false
        appSettings.isFallDetectionEnabled = false
        setDetectionState(FallDetectionState.IDLE)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Update telemetry
            currentAccX = x
            currentAccY = y
            currentAccZ = z
            currentMagnitude = magnitude
            lastSensorTimestamp = System.currentTimeMillis()

            processAcceleration(magnitude)
        }
    }

    private fun processAcceleration(magnitude: Float) {
        val now = System.currentTimeMillis()
        val freeFallThreshold = appSettings.fallFreeFallThreshold
        val impactThreshold = appSettings.fallImpactThreshold

        when (currentState) {
            FallDetectionState.MONITORING_NORMAL -> {
                // Stage 1: Free fall detection (magnitude drops significantly below 9.8 m/s^2)
                if (magnitude < freeFallThreshold) {
                    freeFallStartTime = now
                    lastCandidateTimestamp = now
                    setDetectionState(FallDetectionState.FREE_FALL_CANDIDATE)
                    Log.i(TAG, "[Fall] Stage 1: Free fall candidate detected (magnitude: %.2f m/s²)".format(magnitude))
                }
            }

            FallDetectionState.FREE_FALL_CANDIDATE -> {
                // Stage 2: High-Impact Spike within 1500ms of free fall
                val elapsedSinceFreeFall = now - freeFallStartTime
                if (magnitude > impactThreshold) {
                    impactTime = now
                    setDetectionState(FallDetectionState.IMPACT_DETECTED)
                    Log.i(TAG, "[Fall] Stage 2: Impact spike detected (magnitude: %.2f m/s², delay: %dms)".format(magnitude, elapsedSinceFreeFall))
                    startPostImpactAnalysis()
                } else if (elapsedSinceFreeFall > 1500L) {
                    // Free fall window expired without impact spike -> false positive candidate
                    setDetectionState(FallDetectionState.MONITORING_NORMAL)
                }
            }

            FallDetectionState.IMPACT_DETECTED, FallDetectionState.POST_IMPACT_ANALYSIS -> {
                // Collect post-impact movement samples for 1.8 seconds
                postImpactMagnitudes.add(magnitude)
            }

            FallDetectionState.FALL_CONFIRMED_COUNTDOWN, FallDetectionState.TRIGGERING_SOS, FallDetectionState.IDLE -> {
                // Already in confirmation or idle
            }
        }
    }

    private fun startPostImpactAnalysis() {
        setDetectionState(FallDetectionState.POST_IMPACT_ANALYSIS)
        postImpactMagnitudes.clear()

        postImpactAnalysisRunnable?.let { mainHandler.removeCallbacks(it) }
        postImpactAnalysisRunnable = Runnable {
            // Stage 3: Low movement / resting analysis
            val avgMagnitude = if (postImpactMagnitudes.isNotEmpty()) postImpactMagnitudes.average().toFloat() else 9.8f
            val variance = if (postImpactMagnitudes.isNotEmpty()) {
                val sumDiffSq = postImpactMagnitudes.map { (it - avgMagnitude) * (it - avgMagnitude) }.sum()
                sqrt((sumDiffSq / postImpactMagnitudes.size).toDouble()).toFloat()
            } else 0f

            Log.i(TAG, "[Fall] Stage 3: Post-impact analysis (avg: %.2f, stdDev: %.2f, samples: %d)"
                .format(avgMagnitude, variance, postImpactMagnitudes.size))

            // If user has not resumed high-acceleration continuous motion (stdDev < 4.5 and avg near gravity), confirm fall
            if (variance < 4.5f) {
                onFallConfirmed()
            } else {
                Log.i(TAG, "[Fall] Movement resumed immediately post-impact. False alarm filtered.")
                setDetectionState(FallDetectionState.MONITORING_NORMAL)
            }
        }
        mainHandler.postDelayed(postImpactAnalysisRunnable!!, 1800L)
    }

    private fun onFallConfirmed() {
        lastConfirmedFallTimestamp = System.currentTimeMillis()
        setDetectionState(FallDetectionState.FALL_CONFIRMED_COUNTDOWN)
        Log.w(TAG, "[Fall] ⚠️ FALL CONFIRMED! Launching user confirmation countdown.")

        // Record in safety events database
        val dbHelper = AppDatabaseHelper(this)
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        dbHelper.recordSafetyEvent(
            SafetyEventRecord(
                eventType = "FALL_DETECTED",
                timestamp = lastConfirmedFallTimestamp,
                formattedTime = sdf.format(Date(lastConfirmedFallTimestamp)),
                priority = "CRITICAL",
                details = "High-impact fall detected by multi-stage sensor engine (Impact > %.1f m/s²)".format(appSettings.fallImpactThreshold)
            )
        )

        // Show Heads-Up Alert Notification with Actions
        startConfirmationCountdown()
    }

    private fun simulateFallEvent() {
        Log.i(TAG, "[Fall] 🧪 Fall Simulation Triggered via Test Mode")
        lastCandidateTimestamp = System.currentTimeMillis()
        onFallConfirmed()
    }

    private fun startConfirmationCountdown() {
        confirmationTimer?.cancel()
        val totalSeconds = appSettings.fallConfirmationTimeoutSeconds

        showFallAlertNotification(totalSeconds)

        confirmationTimer = object : CountDownTimer(totalSeconds * 1000L, 1000L) {
            var secondsLeft = totalSeconds
            override fun onTick(millisUntilFinished: Long) {
                secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                updateFallAlertNotification(secondsLeft)
            }

            override fun onFinish() {
                Log.w(TAG, "[Fall] Confirmation timer expired without user response. Escalating to SOS dispatch!")
                triggerFallSOS()
            }
        }.start()
    }

    private fun triggerFallSOS() {
        setDetectionState(FallDetectionState.TRIGGERING_SOS)
        confirmationTimer?.cancel()

        // Cancel the alert notification and restore quiet notification
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(alertNotificationId)

        // Dispatch background SOS broadcast
        val sosIntent = Intent("com.sosence.app.SEND_SOS").apply {
            setPackage(packageName)
            putExtra("IS_FALL", true)
        }
        sendBroadcast(sosIntent)

        // Also notify BackgroundSafetyEngine
        BackgroundSafetyEngine.dispatchEvent(this, SafetyEvent.FallDetected(confidence = 1.0f))

        restoreQuietNotification()
        setDetectionState(FallDetectionState.MONITORING_NORMAL)
    }

    fun cancelFallAlert() {
        Log.i(TAG, "[Fall] ✅ User cancelled fall alert ('I\'m OK')")
        confirmationTimer?.cancel()
        postImpactAnalysisRunnable?.let { mainHandler.removeCallbacks(it) }

        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(alertNotificationId)

        restoreQuietNotification()
        setDetectionState(FallDetectionState.MONITORING_NORMAL)
    }

    private fun setDetectionState(state: FallDetectionState) {
        currentState = state
        currentDetectionState = state
    }

    private fun restoreQuietNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(quietNotificationId, buildQuietNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fall Detection Monitoring",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts and background monitoring for fall detection"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildQuietNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 201, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SOSense Protection Active")
            .setContentText("Continuous Fall Detection is actively monitoring")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setColor(0xFF00ADB5.toInt())
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun showFallAlertNotification(secondsLeft: Int) {
        val notification = buildAlertNotification(secondsLeft)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(alertNotificationId, notification)
    }

    private fun updateFallAlertNotification(secondsLeft: Int) {
        val notification = buildAlertNotification(secondsLeft)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(alertNotificationId, notification)
    }

    private fun buildAlertNotification(secondsLeft: Int): Notification {
        // "I'm OK" Action
        val cancelIntent = Intent(this, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_CANCEL_FALL
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this, 202, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Send SOS Now" Action
        val sendSosIntent = Intent(this, SOSNotificationActionReceiver::class.java).apply {
            action = SOSNotificationActionReceiver.ACTION_TRIGGER_INSTANT_SOS
        }
        val sendSosPendingIntent = PendingIntent.getBroadcast(
            this, 203, sendSosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 204, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("⚠️ Possible Fall Detected!")
            .setContentText("Emergency SOS will be sent in $secondsLeft seconds. Are you OK?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("⚠️ POSSIBLE FALL DETECTED!\n\nA hard impact and sudden rest were detected.\n\nEmergency SOS will automatically dispatch to your trusted contacts in $secondsLeft seconds.\n\nTap 'I'M OK' if you do not need help.")
            )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setColor(0xFFE63946.toInt())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "✅ I'm OK (Cancel)", cancelPendingIntent)
            .addAction(android.R.drawable.ic_dialog_alert, "🚨 Send SOS Now", sendSosPendingIntent)
            .build()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "[Fall] Service onDestroy")
        stopMonitoring()
    }
}
