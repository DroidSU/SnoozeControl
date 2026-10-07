package com.snoozecontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.scheduler.AndroidAlarmScheduler
import com.snoozecontrol.service.AlarmService
import com.snoozecontrol.util.UpcomingAlarmNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        if (intent.action == ACTION_TURN_OFF) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (alarmId != -1) {
                        val db = AlarmDatabase.getDatabase(context)
                        val alarm = db.alarmDao().getAlarmById(alarmId)
                        if (alarm != null) {
                            val isOnce = alarm.isOnce
                            val updatedAlarm = alarm.copy(
                                snoozeCount = 0,
                                snoozedUntilMillis = null,
                                isEnabled = if (isOnce) false else alarm.isEnabled
                            )
                            db.alarmDao().updateAlarm(updatedAlarm)
                            val scheduler = AndroidAlarmScheduler(context)
                            if (isOnce) {
                                scheduler.cancel(updatedAlarm)
                            } else {
                                scheduler.schedule(updatedAlarm)
                            }
                        }
                    }
                    UpcomingAlarmNotificationManager.refreshUpcomingNotification(context)
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error turning off alarm from notification", e)
                } finally {
                    val stopIntent = Intent(context, AlarmService::class.java).apply {
                        action = AlarmService.ACTION_DISMISS
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(stopIntent)
                    } else {
                        context.startService(stopIntent)
                    }
                    pendingResult.finish()
                }
            }
            return
        }

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val ACTION_TURN_OFF = "com.snoozecontrol.ACTION_ALARM_TURN_OFF"
    }
}
