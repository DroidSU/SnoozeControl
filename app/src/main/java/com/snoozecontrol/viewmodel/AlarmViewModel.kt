package com.snoozecontrol.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snoozecontrol.data.AlarmDao
import com.snoozecontrol.model.AlarmItem
import com.snoozecontrol.scheduler.AlarmScheduler
import com.snoozecontrol.util.UpcomingAlarmNotificationManager
import com.snoozecontrol.util.WeatherInfo
import com.snoozecontrol.util.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    application: Application,
    private val alarmDao: AlarmDao,
    private val scheduler: AlarmScheduler
) : AndroidViewModel(application) {

    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo = _weatherInfo.asStateFlow()

    val alarms: StateFlow<List<AlarmItem>> = alarmDao.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val nextAlarm: StateFlow<AlarmItem?> = alarms.map { alarmList ->
        val activeAlarms = alarmList.filter { it.isEnabled }
        if (activeAlarms.isEmpty()) return@map null

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        activeAlarms.sortedWith(compareBy({
            var diff = (it.hour * 60 + it.minute) - (currentHour * 60 + currentMinute)
            if (diff <= 0) diff += 24 * 60
            diff
        })).firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun getCurrentWeather(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _weatherInfo.value = WeatherRepository.fetchCurrentWeather(
                getApplication(),
                forceRefresh = forceRefresh
            )
        }
    }

    init {
        getCurrentWeather()
        viewModelScope.launch {
            nextAlarm.collect { alarm ->
                UpcomingAlarmNotificationManager.updateUpcomingAlarmNotification(
                    getApplication(),
                    alarm
                )
            }
        }
    }

    private var recentlyDeletedAlarm: AlarmItem? = null

    fun toggleAlarm(context: Context, alarmId: Int) {
        viewModelScope.launch {
            val alarm = alarms.value.find { it.id == alarmId } ?: return@launch
            val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
            alarmDao.updateAlarm(updatedAlarm)

            if (updatedAlarm.isEnabled) {
                scheduler.schedule(updatedAlarm)
            } else {
                scheduler.cancel(updatedAlarm)
            }
        }
    }

    fun deleteAlarm(context: Context, alarm: AlarmItem) {
        viewModelScope.launch {
            recentlyDeletedAlarm = alarm
            alarmDao.deleteAlarm(alarm)
            scheduler.cancel(alarm)
        }
    }

    fun undoDelete(context: Context) {
        recentlyDeletedAlarm?.let { alarm ->
            viewModelScope.launch {
                val id = alarmDao.insertAlarm(alarm.copy(id = 0))
                val restoredAlarm = alarm.copy(id = id.toInt())
                if (restoredAlarm.isEnabled) {
                    scheduler.schedule(restoredAlarm)
                }
                recentlyDeletedAlarm = null
            }
        }
    }
}
