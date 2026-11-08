package com.tourregister.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detected_stops")
data class DetectedStop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val arrivalTime: Long,
    val departureTime: Long = 0,
    val isClassified: Boolean = false,
    val date: String = ""
)
