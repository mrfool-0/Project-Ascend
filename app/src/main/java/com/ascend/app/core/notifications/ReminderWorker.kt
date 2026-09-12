package com.ascend.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ascend.app.R
import com.ascend.app.AscendApplication
import com.ascend.app.MainActivity
import com.ascend.app.domain.QuestStatus
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val category = inputData.getString("category") ?: return Result.failure()
        val app = applicationContext as AscendApplication
        val preferences = app.preferences.values.first()
        val enabled = when (category) { "morning" -> preferences.morningNotifications; "workout" -> preferences.workoutNotifications; "nutrition" -> preferences.nutritionNotifications; "evening" -> preferences.eveningNotifications; else -> false }
        if (!preferences.onboardingComplete || !enabled) return Result.success()
        val date = LocalDate.now()
        val state = app.repository.dashboard(date).first()
        val remaining = QuestStatus.pending(state, app.repository.quests().first(), date)
        if (category == "evening" && remaining.isEmpty()) return Result.success()
        val today = app.repository.training.day(date)
        if (category == "workout" && (today.completed || today.template?.isRecovery != false)) return Result.success()
        val (title, text) = when (category) {
            "morning" -> "SYSTEM ONLINE" to "Your daily quests are ready."
            "workout" -> "PRIMARY QUEST AVAILABLE" to (today.template?.name.orEmpty() + " is ready. Open your protocol.")
            "nutrition" -> "NUTRITION OBJECTIVE" to "Review today's fuel and protein progress."
            else -> ("SYSTEM WARNING · " + remaining.size + " OPEN") to
                (remaining.take(8).joinToString("\n") { "• $it" } + "\nReview your open objectives. Complete what is appropriate, or adjust your protocol.")
        }
        val sent = applicationContext.getSharedPreferences("ascend_reminder_delivery", Context.MODE_PRIVATE)
        if (sent.getString(category, null) == date.toString()) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "ASCEND reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_system_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(applicationContext, CHANNEL_ID).setSmallIcon(R.drawable.ic_system_notification).setContentTitle("ASCEND SYSTEM").setContentText("Open ASCEND to review your objectives.").build())
            .setContentIntent(PendingIntent.getActivity(applicationContext, category.hashCode(),
                Intent(applicationContext, MainActivity::class.java).putExtra("ascend_destination", if (category == "nutrition") "nutrition" else if (category == "workout") "training" else "quests").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(category.hashCode(), notification)
            sent.edit { putString(category, date.toString()) }
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
        val delayMinutes = Duration.between(now, next).toMinutes().coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(androidx.work.workDataOf("category" to category))
            .build()
        manager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
