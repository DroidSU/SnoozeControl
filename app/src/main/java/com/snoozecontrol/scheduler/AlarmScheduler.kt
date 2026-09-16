package com.snoozecontrol.scheduler

import com.snoozecontrol.model.AlarmItem

interface AlarmScheduler {
    fun schedule(item: AlarmItem)
    fun cancel(item: AlarmItem)
}
