package com.ascend.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ascend.app.R
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val category = inputData.getString("category") ?: return Result.failure()
        val (title, text) = when (category) {
            "morning" -> "SYSTEM ONLINE" to "Your daily quests are ready."
            "workout" -> "PRIMARY QUEST AVAILABLE" to "Your training protocol is ready."
            "nutrition" -> "NUTRITION OBJECTIVE" to "Review today's fuel and protein progress."
            else -> "DAILY REPORT" to "Review your completed objectives before the day closes."
        }
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "ASCEND reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ascend)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(category.hashCode(), notification)
        }
        return Result.success()
    }

    companion object { const val CHANNEL_ID = "ascend_reminders" }
}

object ReminderScheduler {
    fun configure(context: Context, category: String, enabled: Boolean, hour: Int) {
        val manager = WorkManager.getInstance(context)
        val name = "ascend_reminder_$category"
        if (!enabled) {
            manager.cancelUniqueWork(name)
            return
        }
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMinutes = Duration.between(now, next).toMinutes()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(androidx.work.workDataOf("category" to category))
            .build()
        manager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
