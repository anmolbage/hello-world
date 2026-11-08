package com.tourregister.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stopId: Long,
    val date: String,
    val timeIn: Long,
    val timeOut: Long,
    val durationMinutes: Int,
    val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val purpose: String,
    val notes: String = "",
    val photoPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
