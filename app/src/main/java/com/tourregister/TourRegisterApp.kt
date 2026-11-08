package com.tourregister

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.tourregister.data.database.AppDatabase

class TourRegisterApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val trackingChannel = NotificationChannel(CHANNEL_TRACKING, "Location Tracking", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows when GPS tracking is active"
            setShowBadge(false)
        }
        val stopsChannel = NotificationChannel(CHANNEL_STOPS, "Stop Detection", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Alerts when a new stop is detected"
        }
        val summaryChannel = NotificationChannel(CHANNEL_SUMMARY, "Daily Summary", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Daily tour summary notifications"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(trackingChannel)
        manager.createNotificationChannel(stopsChannel)
        manager.createNotificationChannel(summaryChannel)
    }

    companion object {
        const val CHANNEL_TRACKING = "tracking_channel"
        const val CHANNEL_STOPS = "stops_channel"
        const val CHANNEL_SUMMARY = "summary_channel"
    }
}
