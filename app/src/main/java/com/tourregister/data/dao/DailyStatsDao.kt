package com.tourregister.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.tourregister.data.entity.DailyStats

@Dao
interface DailyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyStats)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getByDate(date: String): DailyStats?

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun getByDateLive(date: String): LiveData<DailyStats?>

    @Query("SELECT SUM(totalDistanceKm) FROM daily_stats WHERE date LIKE :monthPrefix || '%'")
    fun getMonthlyDistance(monthPrefix: String): LiveData<Double?>

    @Query("SELECT SUM(fieldTimeMinutes) FROM daily_stats WHERE date LIKE :monthPrefix || '%'")
    fun getMonthlyFieldMinutes(monthPrefix: String): LiveData<Int?>

    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getStatsInRange(startDate: String, endDate: String): List<DailyStats>
}
