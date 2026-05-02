package com.tourregister.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tourregister.data.dao.*
import com.tourregister.data.entity.*

@Database(
    entities = [DetectedStop::class, Visit::class, LocationPoint::class, DailyStats::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun detectedStopDao(): DetectedStopDao
    abstract fun visitDao(): VisitDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun dailyStatsDao(): DailyStatsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "tour_register.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
