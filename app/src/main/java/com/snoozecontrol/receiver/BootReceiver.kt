package com.snoozecontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.scheduler.AndroidAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
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
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
