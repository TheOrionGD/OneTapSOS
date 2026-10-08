package com.onetapsos.app.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

object LocationHelper {

    private const val TAG = "LocationHelper"
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun isGpsOrNetworkEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gps || network
    }

    /**
     * Checks if GPS / device location service is enabled. If disabled, triggers the native
     * Google Play Services one-tap enable dialog or opens system location settings.
     */
    fun promptEnableLocation(
        activity: Activity,
        resolutionLauncher: ActivityResultLauncher<IntentSenderRequest>? = null,
        onAlreadyEnabled: () -> Unit
    ) {
        if (isGpsOrNetworkEnabled(activity)) {
            onAlreadyEnabled()
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Log.d(TAG, "Location settings satisfied")
            onAlreadyEnabled()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    if (resolutionLauncher != null) {
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                        resolutionLauncher.launch(intentSenderRequest)
                    } else {
                        exception.startResolutionForResult(activity, 1001)
                    }
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Failed to start location resolution: ${sendEx.localizedMessage}")
                    openLocationSettingsDirect(activity)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in location resolution: ${e.localizedMessage}")
                    openLocationSettingsDirect(activity)
                }
            } else {
                openLocationSettingsDirect(activity)
            }
        }
    }

    fun openLocationSettingsDirect(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Toast.makeText(context, "Please enable Location in settings", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open location settings: ${e.localizedMessage}")
        }
    }
}
