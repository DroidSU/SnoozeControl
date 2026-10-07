package com.snoozecontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.scheduler.AndroidAlarmScheduler
import com.snoozecontrol.util.UpcomingAlarmNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpcomingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                when (intent.action) {
                    ACTION_TURN_OFF_ALARM -> {
                        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
                        if (alarmId != -1) {
                            val db = AlarmDatabase.getDatabase(context)
                            val alarm = db.alarmDao().getAlarmById(alarmId)
                            if (alarm != null && alarm.isEnabled) {
                                val scheduler = AndroidAlarmScheduler(context)
                                if (alarm.isOnce) {
                                    val updatedAlarm = alarm.copy(isEnabled = false)
                                    db.alarmDao().updateAlarm(updatedAlarm)
                                    scheduler.cancel(updatedAlarm)
                                } else {
                                    val nextCalendar = alarm.calculateNextCalendar()
                                    val updatedAlarm = alarm.copy(
                                        skippedOccurrenceMillis = nextCalendar.timeInMillis
                                    )
                                    db.alarmDao().updateAlarm(updatedAlarm)
                                    scheduler.schedule(updatedAlarm)
                                }
                            }
                        }
                        UpcomingAlarmNotificationManager.refreshUpcomingNotification(context)
                    }
                    else -> {
                        UpcomingAlarmNotificationManager.refreshUpcomingNotification(context)
                    }
                }
            } catch (e: Exception) {
                Log.e("UpcomingAlarmReceiver", "Error handling upcoming alarm action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SHOW_UPCOMING_NOTIFICATION =
            "com.snoozecontrol.ACTION_SHOW_UPCOMING_NOTIFICATION"
        const val ACTION_TURN_OFF_ALARM =
            "com.snoozecontrol.ACTION_TURN_OFF_ALARM"
        const val EXTRA_ALARM_ID = "ALARM_ID"
    }
}
