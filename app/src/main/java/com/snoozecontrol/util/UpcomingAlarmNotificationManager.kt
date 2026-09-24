package com.snoozecontrol.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.snoozecontrol.MainActivity
import com.snoozecontrol.R
import com.snoozecontrol.model.AlarmItem

object UpcomingAlarmNotificationManager {

    private const val CHANNEL_ID = "UPCOMING_ALARM_CHANNEL"
    private const val NOTIFICATION_ID = 699

    fun updateUpcomingAlarmNotification(context: Context, nextAlarm: AlarmItem?) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            if (nextAlarm == null || !nextAlarm.isEnabled) {
                notificationManager.cancel(NOTIFICATION_ID)
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(
                        "UpcomingAlarmManager",
                        "Notification permission not granted. Skipping notification."
                    )
                    return
                }
            }

            createNotificationChannel(notificationManager)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val text = "Alarm set for ${nextAlarm.displayTime}"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Upcoming Alarm ⏰")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setSound(null)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Throwable) {
            Log.e("UpcomingAlarmManager", "Safely caught error while updating notification", e)
        }
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Upcoming Alarms",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays persistent notification for the next active upcoming alarm."
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
