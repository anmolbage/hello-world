package com.tourregister.data.repository

import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.LiveData
import com.tourregister.data.database.AppDatabase
import com.tourregister.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class TourRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val stopDao = db.detectedStopDao()
    private val visitDao = db.visitDao()
    private val locationDao = db.locationPointDao()
    private val statsDao = db.dailyStatsDao()
    private val geocoder = Geocoder(context, Locale.getDefault())

    suspend fun insertStop(stop: DetectedStop): Long = stopDao.insert(stop)
    suspend fun updateStop(stop: DetectedStop) = stopDao.update(stop)
    suspend fun getStopById(id: Long): DetectedStop? = stopDao.getById(id)
    fun getStopsByDate(date: String): LiveData<List<DetectedStop>> = stopDao.getStopsByDate(date)
    fun getUnclassifiedStops(): LiveData<List<DetectedStop>> = stopDao.getUnclassifiedStops()
    fun getUnclassifiedCount(): LiveData<Int> = stopDao.getUnclassifiedCount()
    suspend fun updateStopDeparture(id: Long, departureTime: Long) = stopDao.updateDepartureTime(id, departureTime)
    suspend fun markStopClassified(id: Long) = stopDao.markClassified(id)
    suspend fun updateStopAddress(id: Long, address: String) = stopDao.updateAddress(id, address)

    suspend fun insertVisit(visit: Visit): Long = visitDao.insert(visit)
    suspend fun updateVisit(visit: Visit) = visitDao.update(visit)
    suspend fun deleteVisit(visit: Visit) = visitDao.delete(visit)
    fun getVisitsByDate(date: String): LiveData<List<Visit>> = visitDao.getVisitsByDate(date)
    suspend fun getVisitsInRange(startDate: String, endDate: String): List<Visit> = visitDao.getVisitsInRange(startDate, endDate)
    fun getAllVisits(): LiveData<List<Visit>> = visitDao.getAllVisits()
    fun getDailyDistance(date: String): LiveData<Double?> = visitDao.getDailyDistance(date)
    fun getMonthlyDistance(monthPrefix: String): LiveData<Double?> = visitDao.getMonthlyDistance(monthPrefix)
    fun getDailyFieldMinutes(date: String): LiveData<Int?> = visitDao.getDailyFieldMinutes(date)
    fun getMonthlyFieldMinutes(monthPrefix: String): LiveData<Int?> = visitDao.getMonthlyFieldMinutes(monthPrefix)

    suspend fun insertLocationPoint(point: LocationPoint) = locationDao.insert(point)
    suspend fun getLastLocationPoint(): LocationPoint? = locationDao.getLastPoint()
    suspend fun getLocationPointsByDate(date: String): List<LocationPoint> = locationDao.getPointsByDate(date)

    suspend fun updateDailyStats(date: String) {
        val visits = visitDao.getVisitsByDateSync(date)
        val totalDistance = visits.sumOf { it.distanceKm }
        val totalMinutes = visits.sumOf { it.durationMinutes }
        val existing = statsDao.getByDate(date)
        val stats = existing?.copy(totalDistanceKm = totalDistance, fieldTimeMinutes = totalMinutes, visitCount = visits.size)
            ?: DailyStats(date = date, totalDistanceKm = totalDistance, fieldTimeMinutes = totalMinutes, visitCount = visits.size)
        statsDao.insertOrUpdate(stats)
    }

    fun getDailyStats(date: String): LiveData<DailyStats?> = statsDao.getByDateLive(date)
    fun getMonthlyDistanceStats(monthPrefix: String): LiveData<Double?> = statsDao.getMonthlyDistance(monthPrefix)
    fun getMonthlyFieldMinutesStats(monthPrefix: String): LiveData<Int?> = statsDao.getMonthlyFieldMinutes(monthPrefix)
    suspend fun getStatsInRange(startDate: String, endDate: String): List<DailyStats> = statsDao.getStatsInRange(startDate, endDate)

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0) ?: "${latitude}, ${longitude}"
            } else "${latitude}, ${longitude}"
        } catch (e: Exception) { "${latitude}, ${longitude}" }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    suspend fun calculateTotalDailyDistance(date: String): Double {
        val points = locationDao.getPointsByDate(date)
        if (points.size < 2) return 0.0
        var totalMeters = 0.0
        for (i in 1 until points.size) {
            totalMeters += calculateDistance(points[i-1].latitude, points[i-1].longitude, points[i].latitude, points[i].longitude).toDouble()
        }
        return totalMeters / 1000.0
    }
}
