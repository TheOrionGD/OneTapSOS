package com.sosence.app.utils

import android.content.Context
import android.location.Location
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

object LiveLocationPublisher {

    private const val TAG = "LiveLocationPublisher"
    private const val RELAY_BASE_URL = "https://ntfy.sh/sosense_live_"
    private const val WEB_VIEWER_BASE_URL = "https://theoriongd.github.io/SOSence/track.html"

    private val scope = CoroutineScope(Dispatchers.IO)

    fun getOrCreateSessionId(context: Context): String {
        val prefs = context.getSharedPreferences("sos_live_prefs", Context.MODE_PRIVATE)
        var sessionId = prefs.getString("current_session_id", null)
        if (sessionId.isNullOrBlank()) {
            sessionId = "sos_" + UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString("current_session_id", sessionId).apply()
        }
        return sessionId
    }

    fun resetSession(context: Context): String {
        val prefs = context.getSharedPreferences("sos_live_prefs", Context.MODE_PRIVATE)
        val newSessionId = "sos_" + UUID.randomUUID().toString().substring(0, 8)
        prefs.edit().putString("current_session_id", newSessionId).apply()
        return newSessionId
    }

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences("sosense_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("user_name", "")?.trim() ?: ""
        return if (name.isNotBlank()) name else "SOSense User"
    }

    fun buildLiveTrackingUrl(
        context: Context,
        location: Location?,
        sosMessage: String? = null,
        customSessionId: String? = null
    ): String {
        val sessionId = customSessionId ?: getOrCreateSessionId(context)
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val name = getUserName(context)
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val msg = sosMessage ?: "I need immediate emergency assistance! Please check on me immediately."
        val encodedMsg = URLEncoder.encode(msg, "UTF-8")

        return "$WEB_VIEWER_BASE_URL?session=$sessionId&lat=$lat&lng=$lng&name=$encodedName&msg=$encodedMsg"
    }

    fun publishLocation(
        context: Context,
        location: Location?,
        isSos: Boolean = true,
        sosMessage: String? = null,
        customSessionId: String? = null
    ) {
        if (location == null) return
        val sessionId = customSessionId ?: getOrCreateSessionId(context)
        val name = getUserName(context)
        val msg = sosMessage ?: "I need immediate emergency assistance! Please check on me immediately."

        val batteryLevel = try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (e: Exception) {
            -1
        }

        scope.launch {
            try {
                val jsonPayload = JSONObject().apply {
                    put("name", name)
                    put("message", msg)
                    put("lat", location.latitude)
                    put("lng", location.longitude)
                    put("speed", if (location.hasSpeed()) location.speed.toDouble() else 0.0)
                    put("accuracy", if (location.hasAccuracy()) location.accuracy.toDouble() else 5.0)
                    put("bearing", if (location.hasBearing()) location.bearing.toDouble() else 0.0)
                    put("battery", batteryLevel)
                    put("timestamp", System.currentTimeMillis())
                    put("isSos", isSos)
                }

                val endpoint = URL("$RELAY_BASE_URL$sessionId")
                val conn = endpoint.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Title", if (isSos) "🚨 SOS Live Movement Update" else "📍 Live Journey Update")

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(jsonPayload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "Location published to $sessionId: code $responseCode")
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to publish live location stream: ${e.message}")
            }
        }
    }
}
