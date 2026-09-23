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
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Failed to schedule exact alarm: ${e.message}")
            requestExactAlarmPermission()
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error scheduling alarm: ${e.message}")
        }
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
        Log.d("AlarmScheduler", "Canceled alarm ${item.id}")
    }

    private fun calculateNextAlarmCalendar(item: AlarmItem): Calendar {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, item.hour)
            set(Calendar.MINUTE, item.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val days = item.selectedDays
        if (days.isEmpty()) {
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar
        }

        val calDayMap = mapOf(
            1 to Calendar.MONDAY,
            2 to Calendar.TUESDAY,
            3 to Calendar.WEDNESDAY,
            4 to Calendar.THURSDAY,
            5 to Calendar.FRIDAY,
            6 to Calendar.SATURDAY,
            7 to Calendar.SUNDAY
        )
        val targetCalDays = days.mapNotNull { calDayMap[it] }.toSet()

        for (i in 0..7) {
            val testCal = calendar.clone() as Calendar
            testCal.add(Calendar.DAY_OF_YEAR, i)
            if (i == 0 && testCal.before(now)) continue
            if (testCal.get(Calendar.DAY_OF_WEEK) in targetCalDays) {
                return testCal
            }
        }

        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar
    }
}
