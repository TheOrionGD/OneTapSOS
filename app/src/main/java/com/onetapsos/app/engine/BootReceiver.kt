package com.onetapsos.app.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Boot broadcast received: $action")

        val pendingResult = goAsync()

        try {
            when (action) {
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                "android.intent.action.LOCKED_BOOT_COMPLETED" -> {
                    BackgroundSafetyEngine.init(context)
                    BackgroundSafetyEngine.restorePendingSchedules(context)
                    BackgroundSafetyEngine.schedulePeriodicBatteryCheck(context)
                    BackgroundSafetyEngine.dispatchEvent(context, SafetyEvent.BootRestored(1))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in BootReceiver: ${e.message}", e)
        } finally {
            pendingResult.finish()
        }
    }
}
