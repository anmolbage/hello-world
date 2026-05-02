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
    fun formatDate(date: String): String = try { displayDateFormat.format(dateFormat.parse(date)!!) } catch (e: Exception) { date }
    fun formatTime(epochMillis: Long): String = timeFormat.format(Date(epochMillis))
    fun formatDateTime(epochMillis: Long): String = dateTimeFormat.format(Date(epochMillis))
    fun formatDuration(minutes: Int): String { val h = minutes / 60; val m = minutes % 60; return if (h > 0) "${h}h ${m}m" else "${m}m" }
    fun formatDurationFromMillis(millis: Long): String = formatDuration(TimeUnit.MILLISECONDS.toMinutes(millis).toInt())
    fun dateFromEpoch(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
    fun durationMinutes(startMillis: Long, endMillis: Long): Int = TimeUnit.MILLISECONDS.toMinutes(endMillis - startMillis).toInt()
    fun formatDistance(km: Double): String = String.format(Locale.getDefault(), "%.1f km", km)
}
