package com.snoozecontrol.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.snoozecontrol.MainActivity
import com.snoozecontrol.R
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.model.AlarmItem
import com.snoozecontrol.receiver.UpcomingAlarmReceiver

object UpcomingAlarmNotificationManager {

    private const val CHANNEL_ID = "UPCOMING_ALARM_CHANNEL"
    private const val NOTIFICATION_ID = 699
    private const val UPCOMING_REFRESH_REQUEST_CODE = 700
    private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L

    suspend fun refreshUpcomingNotification(context: Context) {
        try {
            val db = AlarmDatabase.getDatabase(context)
            val enabledAlarms = db.alarmDao().getEnabledAlarms()

            if (enabledAlarms.isEmpty()) {
                updateUpcomingAlarmNotification(context, null)
                return
            }

            val nowMs = System.currentTimeMillis()
            val nextAlarm = enabledAlarms
                .filter { it.calculateNextCalendar().timeInMillis > nowMs }
                .minByOrNull { it.calculateNextCalendar().timeInMillis }

            updateUpcomingAlarmNotification(context, nextAlarm)
        } catch (e: Throwable) {
            Log.e("UpcomingAlarmManager", "Safely caught error while refreshing notification", e)
        }
    }

    fun updateUpcomingAlarmNotification(context: Context, nextAlarm: AlarmItem?) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

            val refreshIntent = Intent(context, UpcomingAlarmReceiver::class.java).apply {
                action = UpcomingAlarmReceiver.ACTION_SHOW_UPCOMING_NOTIFICATION
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                UPCOMING_REFRESH_REQUEST_CODE,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (nextAlarm == null || !nextAlarm.isEnabled) {
                notificationManager.cancel(NOTIFICATION_ID)
                alarmManager?.cancel(refreshPendingIntent)
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

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val turnOffIntent = Intent(context, UpcomingAlarmReceiver::class.java).apply {
                action = UpcomingAlarmReceiver.ACTION_TURN_OFF_ALARM
                putExtra(UpcomingAlarmReceiver.EXTRA_ALARM_ID, nextAlarm.id)
                data = Uri.parse("snoozecontrol://alarm/upcoming/${nextAlarm.id}")
            }
            val turnOffPendingIntent = PendingIntent.getBroadcast(
                context,
                nextAlarm.id,
                turnOffIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val text = "Alarm set for ${nextAlarm.displayTime}"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Upcoming Alarm ⏰")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .addAction(
                    R.drawable.ic_snooze_control_1,
                    "Turn Off",
                    turnOffPendingIntent
                )
                .setSound(null)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)

            val nextAlarmTimeMs = nextAlarm.calculateNextCalendar().timeInMillis
            val preAlarmRefreshMs = nextAlarmTimeMs - TWO_HOURS_MS
            val nowMs = System.currentTimeMillis()

            if (preAlarmRefreshMs > nowMs && alarmManager != null) {
                try {
                    val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        alarmManager.canScheduleExactAlarms()
                    } else true

                    if (canSchedule) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            preAlarmRefreshMs,
                            refreshPendingIntent
                        )
                        Log.d("UpcomingAlarmManager", "Scheduled 2-hour pre-alarm refresh for $nextAlarmTimeMs")
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            preAlarmRefreshMs,
                            refreshPendingIntent
                        )
                    }
                } catch (e: SecurityException) {
                    Log.e("UpcomingAlarmManager", "SecurityException scheduling exact refresh: ${e.message}")
                    try {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            preAlarmRefreshMs,
                            refreshPendingIntent
                        )
                    } catch (_: Exception) {}
                } catch (e: Exception) {
                    Log.e("UpcomingAlarmManager", "Error scheduling pre-alarm refresh: ${e.message}")
                }
            } else {
                alarmManager?.cancel(refreshPendingIntent)
            }
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
