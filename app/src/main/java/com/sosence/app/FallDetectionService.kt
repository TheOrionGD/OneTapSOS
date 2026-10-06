package com.sosence.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var freeFallDetected = false
    private var freeFallTime = 0L
    private var confirmationTimer: CountDownTimer? = null

    private val channelId = "fall_detection_channel"
    private val notificationId = 2001

    companion object {
        private const val FREE_FALL_THRESHOLD = 3.0f
        private const val IMPACT_THRESHOLD = 20.0f
        private const val FREE_FALL_WINDOW_MS = 1500L
    }

    override fun onCreate() {
        super.onCreate()
        try {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            createNotificationChannel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_CANCEL_FALL") {
            cancelFallAlert()
            return START_STICKY
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(notificationId, buildQuietNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(notificationId, buildQuietNotification())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        val currentTime = System.currentTimeMillis()

        if (magnitude < FREE_FALL_THRESHOLD) {
            freeFallDetected = true
            freeFallTime = currentTime
        }

        if (freeFallDetected && magnitude > IMPACT_THRESHOLD) {
            val timeSinceFreeFall = currentTime - freeFallTime
            if (timeSinceFreeFall in 0..FREE_FALL_WINDOW_MS) {
                onFallDetected()
            }
            freeFallDetected = false
        }

        if (currentTime - freeFallTime > FREE_FALL_WINDOW_MS) {
            freeFallDetected = false
        }
    }

    private fun onFallDetected() {
        showFallConfirmationNotification()
        startConfirmationCountdown()
    }

    private fun startConfirmationCountdown() {
        confirmationTimer?.cancel()
        confirmationTimer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                triggerFallSOS()
            }
        }.start()
    }

    private fun triggerFallSOS() {
        val sosIntent = Intent("com.sosence.app.SEND_SOS").apply {
            setPackage(packageName)
            putExtra("IS_FALL", true)
        }
        sendBroadcast(sosIntent)
        restoreQuietNotification()
    }

    fun cancelFallAlert() {
        confirmationTimer?.cancel()
        restoreQuietNotification()
    }

    private fun restoreQuietNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, buildQuietNotification())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        confirmationTimer?.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fall Detection",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a possible fall is detected"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildQuietNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SOSense")
            .setContentText("Fall monitoring running")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun showFallConfirmationNotification() {
        val cancelIntent = Intent(this, FallCancelReceiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this, 3, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("⚠️ Fall Detected!")
            .setContentText("Sending SOS in 15 seconds. Tap Cancel if you're okay.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "I'm OK - Cancel", cancelPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }
}
