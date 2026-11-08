package com.tourregister.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.tourregister.data.entity.DetectedStop

@Dao
interface DetectedStopDao {
    @Insert
    suspend fun insert(stop: DetectedStop): Long

    @Update
    suspend fun update(stop: DetectedStop)

    @Delete
    suspend fun delete(stop: DetectedStop)

    @Query("SELECT * FROM detected_stops WHERE id = :id")
    suspend fun getById(id: Long): DetectedStop?

    @Query("SELECT * FROM detected_stops WHERE date = :date ORDER BY arrivalTime DESC")
    fun getStopsByDate(date: String): LiveData<List<DetectedStop>>

    @Query("SELECT * FROM detected_stops WHERE date = :date ORDER BY arrivalTime DESC")
    suspend fun getStopsByDateSync(date: String): List<DetectedStop>

    @Query("SELECT * FROM detected_stops WHERE isClassified = 0 ORDER BY arrivalTime DESC")
    fun getUnclassifiedStops(): LiveData<List<DetectedStop>>

    @Query("SELECT COUNT(*) FROM detected_stops WHERE isClassified = 0")
    fun getUnclassifiedCount(): LiveData<Int>

    @Query("SELECT * FROM detected_stops WHERE date = :date AND isClassified = 0 ORDER BY arrivalTime DESC")
    suspend fun getUnclassifiedByDate(date: String): List<DetectedStop>

    @Query("UPDATE detected_stops SET departureTime = :departureTime WHERE id = :id")
    suspend fun updateDepartureTime(id: Long, departureTime: Long)

    @Query("UPDATE detected_stops SET isClassified = 1 WHERE id = :id")
    suspend fun markClassified(id: Long)

    @Query("UPDATE detected_stops SET address = :address WHERE id = :id")
    suspend fun updateAddress(id: Long, address: String)
}
