package com.tourregister.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.tourregister.R
import com.tourregister.TourRegisterApp
import com.tourregister.data.repository.TourRepository
import com.tourregister.ui.main.MainActivity
import com.tourregister.util.DateUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = TourRepository(applicationContext)
        val today = DateUtils.today()
        repository.updateDailyStats(today)
        val totalDistance = repository.calculateTotalDailyDistance(today)
        val stops = repository.getVisitsInRange(today, today)
        val totalMinutes = stops.sumOf { it.durationMinutes }
        val summaryText = "Distance: ${DateUtils.formatDistance(totalDistance)} | Visits: ${stops.size} | Field time: ${DateUtils.formatDuration(totalMinutes)}"

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, TourRegisterApp.CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_tracking).setContentTitle(applicationContext.getString(R.string.daily_summary_title))
            .setContentText(summaryText).setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setContentIntent(pendingIntent).setAutoCancel(true).build()
        applicationContext.getSystemService(android.app.NotificationManager::class.java).notify(2001, notification)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            if (target.before(now)) target.add(Calendar.DAY_OF_MONTH, 1)
            val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(target.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily_summary", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
