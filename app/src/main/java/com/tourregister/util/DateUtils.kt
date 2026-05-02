package com.tourregister.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val monthPrefixFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    fun today(): String = dateFormat.format(Date())
    fun currentMonthPrefix(): String = monthPrefixFormat.format(Date())

    fun formatDate(date: String): String {
        return try {
            val parsed = dateFormat.parse(date)
            if (parsed != null) displayDateFormat.format(parsed) else date
        } catch (e: Exception) { date }
    }

    fun formatTime(epochMillis: Long): String = timeFormat.format(Date(epochMillis))
    fun formatDateTime(epochMillis: Long): String = dateTimeFormat.format(Date(epochMillis))

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    fun formatDurationFromMillis(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis).toInt()
        return formatDuration(minutes)
    }

    fun dateFromEpoch(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

    fun durationMinutes(startMillis: Long, endMillis: Long): Int =
        TimeUnit.MILLISECONDS.toMinutes(endMillis - startMillis).toInt()

    fun formatDistance(km: Double): String =
        String.format(Locale.getDefault(), "%.1f km", km)
}
