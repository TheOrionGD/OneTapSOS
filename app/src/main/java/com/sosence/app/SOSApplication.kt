package com.sosence.app

import android.app.Application
import android.util.Log
import org.osmdroid.config.Configuration

class SOSApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize OSMDroid Map Configuration safely
        try {
            Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = packageName
        } catch (e: Exception) {
            Log.e("SOSApplication", "Failed to configure OSMDroid: ${e.message}")
        }

        // 2. Global Uncaught Exception Guard to prevent abrupt app crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("SOSenseCrashGuard", "Caught unhandled exception in thread ${thread.name}:", throwable)
            } catch (ignored: Exception) {}

            // Pass to default handler or exit cleanly
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
