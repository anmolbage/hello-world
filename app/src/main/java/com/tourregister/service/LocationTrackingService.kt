package com.tourregister.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.tourregister.R
import com.tourregister.TourRegisterApp
import com.tourregister.data.entity.DetectedStop
import com.tourregister.data.entity.LocationPoint
import com.tourregister.data.repository.TourRepository
import com.tourregister.ui.main.MainActivity
import com.tourregister.util.DateUtils
import com.tourregister.util.PrefsManager
import kotlinx.coroutines.*

class LocationTrackingService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var prefs: PrefsManager
    private lateinit var repository: TourRepository
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var lastMovementTime: Long = 0
    private var lastLocation: Location? = null
    private var isStationary = false
    private var currentStopStartTime: Long = 0
    private var isOutsideGeofence = false

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        private const val NOTIFICATION_ID = 1001
        private const val LOCATION_INTERVAL_MS = 30_000L
        private const val FASTEST_INTERVAL_MS = 15_000L
        private const val STATIONARY_THRESHOLD_M = 50f
        private const val STOP_DETECTION_MS = 10 * 60 * 1000L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)
        repository = TourRepository(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        lastMovementTime = System.currentTimeMillis()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) { processLocation(location) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        prefs.setTrackingActive(true)
        startForeground(NOTIFICATION_ID, createNotification("Tour tracking started"))
        requestLocationUpdates()
    }

    private fun stopTracking() {
        prefs.setTrackingActive(false)
        scope.launch { finalizeCurrentStop() }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS).setWaitForAccurateLocation(false).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun processLocation(location: Location) {
        val now = System.currentTimeMillis()
        val today = DateUtils.dateFromEpoch(now)

        scope.launch {
            repository.insertLocationPoint(LocationPoint(
                latitude = location.latitude, longitude = location.longitude,
                timestamp = now, speed = location.speed, accuracy = location.accuracy, date = today
            ))
        }

        val geofenceLat = prefs.getGeofenceLatitude()
        val geofenceLon = prefs.getGeofenceLongitude()
        val geofenceRadius = prefs.getGeofenceRadiusKm() * 1000

        val distFromBranch = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, geofenceLat, geofenceLon, distFromBranch)

        isOutsideGeofence = distFromBranch[0] > geofenceRadius

        if (!isOutsideGeofence) {
            if (isStationary) { scope.launch { finalizeCurrentStop() }; isStationary = false }
            updateNotification("At branch office")
            lastLocation = location; lastMovementTime = now; return
        }

        updateNotification("On field — ${String.format("%.1f", distFromBranch[0] / 1000)} km from branch")

        val prev = lastLocation
        if (prev != null) {
            val dist = location.distanceTo(prev)
            if (dist > STATIONARY_THRESHOLD_M) {
                if (isStationary) { scope.launch { finalizeCurrentStop() }; isStationary = false }
                lastMovementTime = now
            } else {
                if (!isStationary && (now - lastMovementTime) >= STOP_DETECTION_MS) {
                    isStationary = true; currentStopStartTime = lastMovementTime
                    scope.launch { createNewStop(location, currentStopStartTime, today) }
                }
            }
        } else { lastMovementTime = now }
        lastLocation = location
    }

    private suspend fun createNewStop(location: Location, startTime: Long, date: String) {
        val address = repository.reverseGeocode(location.latitude, location.longitude)
        val stop = DetectedStop(latitude = location.latitude, longitude = location.longitude, address = address, arrivalTime = startTime, date = date)
        val id = repository.insertStop(stop)
        prefs.setCurrentStopId(id)
        sendStopNotification(address)
    }

    private suspend fun finalizeCurrentStop() {
        val stopId = prefs.getCurrentStopId()
        if (stopId > 0) { repository.updateStopDeparture(stopId, System.currentTimeMillis()); prefs.clearCurrentStop() }
    }

    private fun sendStopNotification(address: String) {
        val intent = Intent(this, MainActivity::class.java).apply { putExtra("navigate_to", "stops"); flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, TourRegisterApp.CHANNEL_STOPS)
            .setSmallIcon(R.drawable.ic_stop_notification).setContentTitle(getString(R.string.stop_detected))
            .setContentText("Stopped at: $address").setContentIntent(pendingIntent).setAutoCancel(true).build()
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, TourRegisterApp.CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_tracking).setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(text).setContentIntent(pendingIntent).setOngoing(true).setSilent(true).build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy(); scope.cancel(); fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
