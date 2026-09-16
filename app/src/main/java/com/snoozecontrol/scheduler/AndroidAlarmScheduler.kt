package com.snoozecontrol.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                Toast.makeText(context, "Please allow exact alarms for this app to work", Toast.LENGTH_LONG).show()
                context.startActivity(intent)
                return
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", item.id)
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, item.hour)
            set(Calendar.MINUTE, item.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, com.snoozecontrol.MainActivity::class.java).apply {
            putExtra("ALARM_ID", item.id)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            item.id,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            calendar.timeInMillis,
            showPendingIntent
        )

        alarmManager.setAlarmClock(
            alarmClockInfo,
            pendingIntent
        )
    }

    override fun cancel(item: AlarmItem) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                item.id,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}
