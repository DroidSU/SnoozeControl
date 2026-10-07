package com.snoozecontrol.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.snoozecontrol.MainActivity
import com.snoozecontrol.model.AlarmItem
import com.snoozecontrol.receiver.AlarmReceiver
import com.snoozecontrol.receiver.BedtimeReceiver
import java.util.Calendar

class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(item: AlarmItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                requestExactAlarmPermission()
                return
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", item.id)
        }

        val calendar = calculateNextAlarmCalendar(item)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("ALARM_ID", item.id)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            item.id,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                calendar.timeInMillis,
                showPendingIntent
            )

            alarmManager.setAlarmClock(
                alarmClockInfo,
                pendingIntent
            )
            Log.d("AlarmScheduler", "Scheduled alarm ${item.id} for ${calendar.time}")

            if (item.isBedtimeReminderEnabled) {
                scheduleBedtimeReminder(item, calendar)
            } else {
                cancelBedtimeReminder(item)
            }
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Failed to schedule exact alarm: ${e.message}")
            requestExactAlarmPermission()
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error scheduling alarm: ${e.message}")
        }
    }

    private fun scheduleBedtimeReminder(item: AlarmItem, alarmCal: Calendar) {
        val bedtimeCal = (alarmCal.clone() as Calendar).apply {
            add(Calendar.HOUR_OF_DAY, -8)
        }

        if (bedtimeCal.after(Calendar.getInstance())) {
            val intent = Intent(context, BedtimeReceiver::class.java).apply {
                putExtra("ALARM_TIME_DISPLAY", item.displayTime)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.id + BEDTIME_ID_OFFSET,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    bedtimeCal.timeInMillis,
                    pendingIntent
                )
                Log.d(
                    "AlarmScheduler",
                    "Scheduled bedtime reminder for alarm ${item.id} at ${bedtimeCal.time}"
                )
            } catch (e: Exception) {
                Log.e("AlarmScheduler", "Failed to schedule bedtime reminder: ${e.message}")
            }
        }
    }

    private fun cancelBedtimeReminder(item: AlarmItem) {
        val intent = Intent(context, BedtimeReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id + BEDTIME_ID_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Toast.makeText(
                context,
                "Exact alarm permission required for reliable wake-up",
                Toast.LENGTH_LONG
            ).show()
            context.startActivity(intent)
        }
    }

    override fun cancel(item: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        cancelBedtimeReminder(item)
        Log.d("AlarmScheduler", "Canceled alarm ${item.id}")
    }

    companion object {
        private const val BEDTIME_ID_OFFSET = 100000
    }

    fun calculateNextAlarmCalendar(item: AlarmItem): Calendar {
        return item.calculateNextCalendar()
    }
}
