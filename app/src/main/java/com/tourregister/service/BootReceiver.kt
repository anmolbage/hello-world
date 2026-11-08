package com.tourregister.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tourregister.util.PrefsManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = PrefsManager(context)
            if (prefs.isTrackingActive()) {
                context.startForegroundService(Intent(context, LocationTrackingService::class.java).apply { action = LocationTrackingService.ACTION_START })
            }
        }
    }
}
