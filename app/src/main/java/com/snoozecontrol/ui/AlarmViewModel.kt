package com.snoozecontrol.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snoozecontrol.R
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.model.AlarmItem
import com.snoozecontrol.model.ChallengeType
import com.snoozecontrol.model.DismissState
import com.snoozecontrol.scheduler.AndroidAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()
    
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

    private var recentlyDeletedAlarm: AlarmItem? = null

    fun addAlarm(context: Context, hour: Int, minute: Int, challengeType: ChallengeType = ChallengeType.MATH, targetBarcode: String? = null) {
        viewModelScope.launch {
            val newAlarm = AlarmItem(
                hour = hour,
                minute = minute,
                isEnabled = true,
                challengeType = challengeType,
                targetBarcode = targetBarcode
            )
            val id = alarmDao.insertAlarm(newAlarm)
            AndroidAlarmScheduler(context).schedule(newAlarm.copy(id = id.toInt()))
        }
    }

    fun updateAlarmTime(
        context: Context,
        alarmId: Int,
        hour: Int,
        minute: Int,
        challengeType: ChallengeType,
        targetBarcode: String?
    ) {
        viewModelScope.launch {
            val alarm = alarmDao.getAlarmById(alarmId) ?: return@launch
            val updatedAlarm = alarm.copy(
                hour = hour,
                minute = minute,
                challengeType = challengeType,
                targetBarcode = targetBarcode
            )
            alarmDao.updateAlarm(updatedAlarm)
            if (updatedAlarm.isEnabled) {
                AndroidAlarmScheduler(context).schedule(updatedAlarm)
            }
        }
    }

    fun toggleAlarm(context: Context, alarmId: Int) {
        viewModelScope.launch {
            val alarm = alarms.value.find { it.id == alarmId } ?: return@launch
            val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
            alarmDao.updateAlarm(updatedAlarm)
            
            val scheduler = AndroidAlarmScheduler(context)
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
            AndroidAlarmScheduler(context).cancel(alarm)
        }
    }

    fun undoDelete(context: Context) {
        recentlyDeletedAlarm?.let { alarm ->
            viewModelScope.launch {
                val id = alarmDao.insertAlarm(alarm.copy(id = 0))
                val restoredAlarm = alarm.copy(id = id.toInt())
                if (restoredAlarm.isEnabled) {
                    AndroidAlarmScheduler(context).schedule(restoredAlarm)
                }
                recentlyDeletedAlarm = null
            }
        }
    }

    private val _dismissState = MutableStateFlow(DismissState())
    val dismissState: StateFlow<DismissState> = _dismissState.asStateFlow()

    fun triggerAlarm(alarmId: Int) {
        if (_dismissState.value.challengeType != ChallengeType.NONE) return

        viewModelScope.launch {
            val alarm = alarmDao.getAlarmById(alarmId)
            if (alarm != null) {
                when (alarm.challengeType) {
                    ChallengeType.MATH -> generateNewMathProblem()
                    ChallengeType.BARCODE -> {
                        _dismissState.update {
                            it.copy(
                                challengeType = ChallengeType.BARCODE,
                                targetBarcode = alarm.targetBarcode,
                                isScanning = true
                            )
                        }
                    }
                    ChallengeType.NONE -> {
                        _dismissState.update {
                            it.copy(challengeType = ChallengeType.NONE)
                        }
                    }
                }
            } else {
                generateNewMathProblem()
            }
        }
    }

    fun generateNewMathProblem() {
        val num1 = (1..20).random()
        val num2 = (1..20).random()
        val operator = listOf("+", "-", "*").random()
        
        val (equation, answer) = when (operator) {
            "+" -> "$num1 + $num2" to (num1 + num2)
            "-" -> "$num1 - $num2" to (num1 - num2)
            "*" -> "$num1 * $num2" to (num1 * num2)
            else -> "$num1 + $num2" to (num1 + num2)
        }
        
        _dismissState.value = DismissState(
            challengeType = ChallengeType.MATH,
            equation = equation, 
            correctAnswer = answer
        )
    }

    fun onBarcodeScanned(value: String, onSuccess: () -> Unit) {
        val currentState = _dismissState.value
        if (currentState.challengeType == ChallengeType.BARCODE) {
            if (value == currentState.targetBarcode) {
                onSuccess()
                _dismissState.value = DismissState()
            } else {
                _dismissState.update {
                    it.copy(
                        error = getApplication<Application>().getString(
                            R.string.error_wrong_barcode,
                            value
                        )
                    )
                }
            }
        }
    }

    fun onAnswerChange(newInput: String) {
        _dismissState.update { it.copy(input = newInput, error = null) }
    }

    fun checkAnswer(onSuccess: () -> Unit) {
        val currentState = _dismissState.value
        val userTypedAnswer = currentState.input.toIntOrNull()
        
        if (userTypedAnswer == currentState.correctAnswer) {
            onSuccess()
            _dismissState.value = DismissState()
        } else {
            _dismissState.update {
                it.copy(
                    error = getApplication<Application>().getString(R.string.error_incorrect_answer),
                    input = ""
                )
            }
        }
    }
}
