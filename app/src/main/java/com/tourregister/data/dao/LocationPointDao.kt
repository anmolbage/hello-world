package com.tourregister.data.dao

import androidx.room.*
import com.tourregister.data.entity.LocationPoint

@Dao
interface LocationPointDao {
    @Insert
    suspend fun insert(point: LocationPoint)

    @Query("SELECT * FROM location_points WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getPointsByDate(date: String): List<LocationPoint>

    @Query("SELECT * FROM location_points ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPoint(): LocationPoint?

    @Query("DELETE FROM location_points WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)
}
