package com.tourregister.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val date: String,
    val totalDistanceKm: Double = 0.0,
    val fieldTimeMinutes: Int = 0,
    val visitCount: Int = 0,
    val trackingStartTime: Long = 0,
    val trackingEndTime: Long = 0
)
