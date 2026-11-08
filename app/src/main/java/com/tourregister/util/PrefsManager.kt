package com.tourregister.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PrefsManager(context: Context) {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context, "tour_register_secure_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val prefs: SharedPreferences = context.getSharedPreferences("tour_register_prefs", Context.MODE_PRIVATE)

    fun isPinSet(): Boolean = securePrefs.contains(KEY_PIN)
    fun setPin(pin: String) { securePrefs.edit().putString(KEY_PIN, pin).apply() }
    fun verifyPin(pin: String): Boolean = securePrefs.getString(KEY_PIN, null) == pin

    fun isGeofenceSet(): Boolean = prefs.contains(KEY_GEOFENCE_LAT)
    fun setGeofence(latitude: Double, longitude: Double, radiusKm: Float) {
        prefs.edit().putFloat(KEY_GEOFENCE_LAT, latitude.toFloat()).putFloat(KEY_GEOFENCE_LON, longitude.toFloat()).putFloat(KEY_GEOFENCE_RADIUS, radiusKm).apply()
    }
    fun getGeofenceLatitude(): Double = prefs.getFloat(KEY_GEOFENCE_LAT, 0f).toDouble()
    fun getGeofenceLongitude(): Double = prefs.getFloat(KEY_GEOFENCE_LON, 0f).toDouble()
    fun getGeofenceRadiusKm(): Float = prefs.getFloat(KEY_GEOFENCE_RADIUS, 1.0f)

    fun isTrackingActive(): Boolean = prefs.getBoolean(KEY_TRACKING_ACTIVE, false)
    fun setTrackingActive(active: Boolean) { prefs.edit().putBoolean(KEY_TRACKING_ACTIVE, active).apply() }
    fun setCurrentStopId(stopId: Long) { prefs.edit().putLong(KEY_CURRENT_STOP_ID, stopId).apply() }
    fun getCurrentStopId(): Long = prefs.getLong(KEY_CURRENT_STOP_ID, -1)
    fun clearCurrentStop() { prefs.edit().remove(KEY_CURRENT_STOP_ID).apply() }
    fun setUserName(name: String) { prefs.edit().putString(KEY_USER_NAME, name).apply() }
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "Branch Manager") ?: "Branch Manager"
    fun setBranchName(name: String) { prefs.edit().putString(KEY_BRANCH_NAME, name).apply() }
    fun getBranchName(): String = prefs.getString(KEY_BRANCH_NAME, "") ?: ""

    companion object {
        private const val KEY_PIN = "user_pin"
        private const val KEY_GEOFENCE_LAT = "geofence_lat"
        private const val KEY_GEOFENCE_LON = "geofence_lon"
        private const val KEY_GEOFENCE_RADIUS = "geofence_radius_km"
        private const val KEY_TRACKING_ACTIVE = "tracking_active"
        private const val KEY_CURRENT_STOP_ID = "current_stop_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_BRANCH_NAME = "branch_name"
    }
}
