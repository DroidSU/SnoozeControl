package com.snoozecontrol.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.model.AlarmItem
import com.snoozecontrol.model.ChallengeType
import com.snoozecontrol.scheduler.AndroidAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class AddEditAlarmUiState(
    val alarmId: Int = -1,
    val hour12: Int = 7,
    val minute: Int = 0,
    val isAm: Boolean = true,
    val challengeType: ChallengeType = ChallengeType.MATH,
    val targetBarcode: String? = null,
    val selectedDays: Set<Int> = emptySet(),
    val snoozeDurationMinutes: Int = 5,
    val isBedtimeReminderEnabled: Boolean = true,
    val isLoaded: Boolean = false
) {
    val final24Hour: Int
        get() = when {
            isAm && hour12 == 12 -> 0
            isAm -> hour12
            !isAm && hour12 == 12 -> 12
            else -> hour12 + 12
        }

    val repeatDaysString: String
        get() = selectedDays.sorted().joinToString(",")

    val isSaveEnabled: Boolean
        get() = challengeType != ChallengeType.BARCODE || targetBarcode != null
}

class AddEditAlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()

    private val _uiState = MutableStateFlow(AddEditAlarmUiState())
    val uiState: StateFlow<AddEditAlarmUiState> = _uiState.asStateFlow()

    fun initAlarm(alarm: AlarmItem?, barcodeResult: String? = null) {
        if (_uiState.value.isLoaded) {
            if (barcodeResult != null) {
                _uiState.update { it.copy(targetBarcode = barcodeResult) }
            }
            return
        }

        if (alarm != null) {
            val isAm = alarm.hour < 12
            val hour12 = when {
                alarm.hour == 0 -> 12
                alarm.hour > 12 -> alarm.hour - 12
                else -> alarm.hour
            }
            val days = if (alarm.repeatDays.isBlank()) emptySet()
            else alarm.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

            _uiState.value = AddEditAlarmUiState(
                alarmId = alarm.id,
                hour12 = hour12,
                minute = alarm.minute,
                isAm = isAm,
                challengeType = alarm.challengeType,
                targetBarcode = barcodeResult ?: alarm.targetBarcode,
                selectedDays = days,
                snoozeDurationMinutes = alarm.snoozeDurationMinutes,
                isBedtimeReminderEnabled = alarm.isBedtimeReminderEnabled,
                isLoaded = true
            )
        } else {
            // Default time for new alarm (now + 1 min)
            val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }
            val h24 = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)
            val isAm = h24 < 12
            val h12 = when {
                h24 == 0 -> 12
                h24 > 12 -> h24 - 12
                else -> h24
            }

            _uiState.value = AddEditAlarmUiState(
                alarmId = -1,
                hour12 = h12,
                minute = min,
                isAm = isAm,
                challengeType = ChallengeType.MATH,
                targetBarcode = barcodeResult,
                selectedDays = emptySet(),
                snoozeDurationMinutes = 5,
                isBedtimeReminderEnabled = true,
                isLoaded = true
            )
        }
    }

    fun onHourChange(hour12: Int) {
        _uiState.update { it.copy(hour12 = hour12) }
    }

    fun onMinuteChange(minute: Int) {
        _uiState.update { it.copy(minute = minute) }
    }

    fun onAmPmChange(isAm: Boolean) {
        _uiState.update { it.copy(isAm = isAm) }
    }

    fun onChallengeTypeChange(type: ChallengeType) {
        _uiState.update { it.copy(challengeType = type) }
    }

    fun onBarcodeResult(barcode: String?) {
        if (barcode != null) {
            _uiState.update { it.copy(targetBarcode = barcode) }
        }
    }

    fun onDaysChange(days: Set<Int>) {
        _uiState.update { it.copy(selectedDays = days) }
    }

    fun onSnoozeDurationChange(minutes: Int) {
        _uiState.update { it.copy(snoozeDurationMinutes = minutes) }
    }

    fun onBedtimeReminderToggle(enabled: Boolean) {
        _uiState.update { it.copy(isBedtimeReminderEnabled = enabled) }
    }

    fun saveAlarm(context: Context, onSaved: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            val alarmItem = AlarmItem(
                id = if (state.alarmId == -1) 0 else state.alarmId,
                hour = state.final24Hour,
                minute = state.minute,
                isEnabled = true,
                challengeType = state.challengeType,
                targetBarcode = state.targetBarcode,
                repeatDays = state.repeatDaysString,
                snoozeDurationMinutes = state.snoozeDurationMinutes,
                maxSnoozeCount = 3,
                isBedtimeReminderEnabled = state.isBedtimeReminderEnabled
            )

            if (state.alarmId == -1) {
                val newId = alarmDao.insertAlarm(alarmItem)
                AndroidAlarmScheduler(context).schedule(alarmItem.copy(id = newId.toInt()))
            } else {
                alarmDao.updateAlarm(alarmItem)
                AndroidAlarmScheduler(context).schedule(alarmItem)
            }
            onSaved()
        }
    }
}
