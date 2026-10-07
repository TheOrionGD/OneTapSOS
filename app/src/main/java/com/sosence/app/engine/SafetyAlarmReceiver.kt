package com.sosence.app.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SafetyAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SafetyAlarmReceiver"

        const val ACTION_SAFETY_TIMER_EXPIRED = "com.sosence.app.ACTION_SAFETY_TIMER_EXPIRED"
        const val ACTION_CHECKIN_EXPIRED = "com.sosence.app.ACTION_CHECKIN_EXPIRED"

        const val EXTRA_REASON = "EXTRA_REASON"
        const val EXTRA_MINUTES = "EXTRA_MINUTES"
        const val EXTRA_NOTE = "EXTRA_NOTE"

        const val REQ_CODE_SAFETY_TIMER = 4001
        const val REQ_CODE_CHECKIN = 4002
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Safety alarm fired: $action")

        val pendingResult = goAsync()

        try {
            when (action) {
                ACTION_SAFETY_TIMER_EXPIRED -> {
                    val reason = intent.getStringExtra(EXTRA_REASON) ?: "Safety Monitoring"
                    val minutes = intent.getIntExtra(EXTRA_MINUTES, 30)
                    BackgroundSafetyEngine.dispatchEvent(
                        context,
                        SafetyEvent.SafetyTimerExpired(reason, minutes)
                    )
                }
                ACTION_CHECKIN_EXPIRED -> {
                    val note = intent.getStringExtra(EXTRA_NOTE) ?: "Scheduled Check-In"
                    BackgroundSafetyEngine.dispatchEvent(
                        context,
                        SafetyEvent.CheckInExpired(note)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing safety alarm: ${e.message}", e)
        } finally {
            pendingResult.finish()
        }
    }
}
