package com.snoozecontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.scheduler.AndroidAlarmScheduler
import com.snoozecontrol.util.UpcomingAlarmNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                try {
                    val db = AlarmDatabase.getDatabase(context)
                    val enabledAlarms = db.alarmDao().getEnabledAlarms()
                    val scheduler = AndroidAlarmScheduler(context)

                    enabledAlarms.forEach { alarm ->
                        scheduler.schedule(alarm)
                    }

                    UpcomingAlarmNotificationManager.refreshUpcomingNotification(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
