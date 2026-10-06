package com.sosence.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FallCancelReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // Cancel the fall confirmation notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2001)

        // Tell the FallDetectionService to stop the countdown
        val serviceIntent = Intent(context, FallDetectionService::class.java).apply {
            action = "ACTION_CANCEL_FALL"
        }
        context.startService(serviceIntent)
    }
}
