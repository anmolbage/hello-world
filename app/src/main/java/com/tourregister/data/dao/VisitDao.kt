package com.tourregister.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.tourregister.data.entity.Visit

@Dao
interface VisitDao {
    @Insert
    suspend fun insert(visit: Visit): Long

    @Update
    suspend fun update(visit: Visit)

    @Delete
    suspend fun delete(visit: Visit)

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun getById(id: Long): Visit?

    @Query("SELECT * FROM visits WHERE date = :date ORDER BY timeIn ASC")
    fun getVisitsByDate(date: String): LiveData<List<Visit>>

    @Query("SELECT * FROM visits WHERE date = :date ORDER BY timeIn ASC")
    suspend fun getVisitsByDateSync(date: String): List<Visit>

    @Query("SELECT * FROM visits WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, timeIn ASC")
    suspend fun getVisitsInRange(startDate: String, endDate: String): List<Visit>

    @Query("SELECT * FROM visits ORDER BY date DESC, timeIn DESC")
    fun getAllVisits(): LiveData<List<Visit>>

    @Query("SELECT SUM(distanceKm) FROM visits WHERE date = :date")
    fun getDailyDistance(date: String): LiveData<Double?>

    @Query("SELECT SUM(distanceKm) FROM visits WHERE date LIKE :monthPrefix || '%'")
    fun getMonthlyDistance(monthPrefix: String): LiveData<Double?>

    @Query("SELECT SUM(durationMinutes) FROM visits WHERE date = :date")
    fun getDailyFieldMinutes(date: String): LiveData<Int?>

    @Query("SELECT SUM(durationMinutes) FROM visits WHERE date LIKE :monthPrefix || '%'")
    fun getMonthlyFieldMinutes(monthPrefix: String): LiveData<Int?>

    @Query("SELECT COUNT(*) FROM visits WHERE date = :date")
    fun getDailyVisitCount(date: String): LiveData<Int>

    @Query("SELECT DISTINCT date FROM visits WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getVisitDatesInRange(startDate: String, endDate: String): List<String>
}
