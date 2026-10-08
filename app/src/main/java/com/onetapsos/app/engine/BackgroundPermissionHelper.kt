package com.onetapsos.app.engine

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object BackgroundPermissionHelper {

    const val REQ_CODE_NOTIFICATIONS = 701
    const val REQ_CODE_EXACT_ALARM = 702
    const val REQ_CODE_BATTERY_OPT = 703

    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_CODE_NOTIFICATIONS
            )
        }
    }

    fun checkNotificationWithRationale(activity: Activity, onGranted: (() -> Unit)? = null) {
        if (isNotificationPermissionGranted(activity)) {
            onGranted?.invoke()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.appcompat.app.AlertDialog.Builder(activity, com.google.android.material.R.style.Theme_MaterialComponents_Dialog_Alert)
                .setTitle("🔔 Notification Permission Required")
                .setMessage("OneTapSOS needs notification permission to send critical low battery warnings and emergency SOS alerts when the app is closed.")
                .setPositiveButton("Allow Notifications") { dialog, _ ->
                    dialog.dismiss()
                    requestNotificationPermission(activity)
                }
                .setNegativeButton("Later") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    fun isExactAlarmPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    fun requestExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_SETTINGS)
                activity.startActivity(intent)
            }
        }
    }

    fun checkExactAlarmWithRationale(activity: Activity, onGranted: (() -> Unit)? = null) {
        if (isExactAlarmPermissionGranted(activity)) {
            onGranted?.invoke()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            androidx.appcompat.app.AlertDialog.Builder(activity, com.google.android.material.R.style.Theme_MaterialComponents_Dialog_Alert)
                .setTitle("⏱️ Exact Alarm Permission Required")
                .setMessage("To ensure Safety Timers and Check-In alarms fire precisely even when your phone is locked and sleeping (Doze mode), please grant Exact Alarm access to OneTapSOS.")
                .setPositiveButton("Open Settings") { dialog, _ ->
                    dialog.dismiss()
                    requestExactAlarmPermission(activity)
                }
                .setNegativeButton("Proceed Anyway") { dialog, _ ->
                    dialog.dismiss()
                    onGranted?.invoke()
                }
                .show()
        } else {
            onGranted?.invoke()
        }
    }

    fun isBatteryOptimizationExempt(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }

    fun requestBatteryOptimizationExemption(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    activity.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    fun checkBatteryOptimizationWithRationale(activity: Activity, onGranted: (() -> Unit)? = null) {
        if (isBatteryOptimizationExempt(activity)) {
            onGranted?.invoke()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            androidx.appcompat.app.AlertDialog.Builder(activity, com.google.android.material.R.style.Theme_MaterialComponents_Dialog_Alert)
                .setTitle("⚡ Disable Battery Optimization")
                .setMessage("Android power management can delay or suspend background safety monitoring when the phone is idle. Allow OneTapSOS to run unrestricted in the background for continuous safety.")
                .setPositiveButton("Optimize Exemption") { dialog, _ ->
                    dialog.dismiss()
                    requestBatteryOptimizationExemption(activity)
                }
                .setNegativeButton("Keep Standard") { dialog, _ ->
                    dialog.dismiss()
                    onGranted?.invoke()
                }
                .show()
        } else {
            onGranted?.invoke()
        }
    }
}
