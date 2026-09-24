package com.snoozecontrol.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snoozecontrol.R
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.model.ChallengeType
import com.snoozecontrol.scheduler.AndroidAlarmScheduler
import com.snoozecontrol.util.Quote
import com.snoozecontrol.util.QuoteProvider
import com.snoozecontrol.util.WeatherInfo
import com.snoozecontrol.util.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class AlarmDismissUiState(
    val alarmId: Int = -1,
    val challengeType: ChallengeType = ChallengeType.NONE,
    val equation: String = "",
    val correctAnswer: Int = 0,
    val answerInput: String = "",
    val targetBarcode: String? = null,
    val errorMessage: String? = null,
    val snoozeDurationMinutes: Int = 5,
    val maxSnoozeCount: Int = 3,
    val snoozeCount: Int = 0,
    val isDismissed: Boolean = false,
    val isSnoozed: Boolean = false,
    val showMorningDashboard: Boolean = false,
    val isWeatherLoading: Boolean = false,
    val quote: Quote = QuoteProvider.getTodayQuote()
) {
    val canSnooze: Boolean
        get() = snoozeCount < maxSnoozeCount && maxSnoozeCount > 0

    val remainingSnoozes: Int
        get() = (maxSnoozeCount - snoozeCount).coerceAtLeast(0)
}

class AlarmDismissViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()

    private val _uiState = MutableStateFlow(AlarmDismissUiState())
    val uiState: StateFlow<AlarmDismissUiState> = _uiState.asStateFlow()

    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo = _weatherInfo.asStateFlow()

    init {
        getCurrentWeather()
    }

    fun getCurrentWeather() {
        viewModelScope.launch {
            _weatherInfo.value = WeatherRepository.fetchCurrentWeather(getApplication())
            _uiState.update { it.copy(isWeatherLoading = false) }
        }
    }

    fun loadDismissChallenge(alarmId: Int) {
        viewModelScope.launch {
            val alarm = if (alarmId != -1) alarmDao.getAlarmById(alarmId) else null
            if (alarm != null) {
                val snoozeDur = alarm.snoozeDurationMinutes
                val maxSnooze = alarm.maxSnoozeCount
                val currentSnooze = alarm.snoozeCount

                when (alarm.challengeType) {
                    ChallengeType.MATH -> generateMathProblem(alarmId, snoozeDur, maxSnooze, currentSnooze)
                    ChallengeType.BARCODE -> {
                        _uiState.value = AlarmDismissUiState(
                            alarmId = alarmId,
                            challengeType = ChallengeType.BARCODE,
                            targetBarcode = alarm.targetBarcode,
                            snoozeDurationMinutes = snoozeDur,
                            maxSnoozeCount = maxSnooze,
                            snoozeCount = currentSnooze
                        )
                    }

                    ChallengeType.NONE -> {
                        _uiState.value = AlarmDismissUiState(
                            alarmId = alarmId,
                            challengeType = ChallengeType.NONE,
                            snoozeDurationMinutes = snoozeDur,
                            maxSnoozeCount = maxSnooze,
                            snoozeCount = currentSnooze
                        )
                    }
                }
            } else {
                generateMathProblem(alarmId, 5, 3, 0)
            }
        }
    }

    private fun generateMathProblem(
        alarmId: Int,
        snoozeDur: Int,
        maxSnooze: Int,
        currentSnooze: Int
    ) {
        val num1 = (1..20).random()
        val num2 = (1..20).random()
        val operator = listOf("+", "-", "*").random()

        val (equation, answer) = when (operator) {
            "+" -> "$num1 + $num2" to (num1 + num2)
            "-" -> "$num1 - $num2" to (num1 - num2)
            "*" -> "$num1 * $num2" to (num1 * num2)
            else -> "$num1 + $num2" to (num1 + num2)
        }

        _uiState.value = AlarmDismissUiState(
            alarmId = alarmId,
            challengeType = ChallengeType.MATH,
            equation = equation,
            correctAnswer = answer,
            snoozeDurationMinutes = snoozeDur,
            maxSnoozeCount = maxSnooze,
            snoozeCount = currentSnooze
        )
    }

    fun onAnswerChange(newInput: String) {
        _uiState.update { it.copy(answerInput = newInput, errorMessage = null) }
    }

    fun checkMathAnswer(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val userTypedAnswer = currentState.answerInput.trim().toIntOrNull()

        if (userTypedAnswer == currentState.correctAnswer) {
            resetSnoozeCountAndDismiss(currentState.alarmId)
            onSuccess()
        } else {
            _uiState.update {
                it.copy(
                    errorMessage = getApplication<Application>().getString(R.string.error_incorrect_answer),
                    answerInput = ""
                )
            }
        }
    }

    fun onBarcodeScanned(scannedBarcode: String, onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (currentState.challengeType == ChallengeType.BARCODE) {
            val scannedClean = scannedBarcode.trim()
            val targetClean = currentState.targetBarcode?.trim().orEmpty()

            if (scannedClean.isNotEmpty() && scannedClean == targetClean) {
                resetSnoozeCountAndDismiss(currentState.alarmId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = getApplication<Application>().getString(
                            R.string.error_wrong_barcode,
                            scannedClean
                        )
                    )
                }
            }
        }
    }

    private fun resetSnoozeCountAndDismiss(alarmId: Int) {
        viewModelScope.launch {
            if (alarmId != -1) {
                val alarm = alarmDao.getAlarmById(alarmId)
                if (alarm != null) {
                    val isOnce = alarm.isOnce
                    val updatedAlarm = alarm.copy(
                        snoozeCount = 0,
                        isEnabled = if (isOnce) false else alarm.isEnabled
                    )
                    alarmDao.updateAlarm(updatedAlarm)
                    if (isOnce) {
                        AndroidAlarmScheduler(getApplication()).cancel(updatedAlarm)
                    }
                }
            }
            _uiState.update {
                it.copy(
                    isDismissed = true,
                    showMorningDashboard = true,
                    isWeatherLoading = true
                )
            }
        }
    }

    fun snoozeAlarm(context: Context, onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (!currentState.canSnooze || currentState.alarmId == -1) return

        viewModelScope.launch {
            val alarm = alarmDao.getAlarmById(currentState.alarmId)
            if (alarm != null) {
                val updatedSnoozeCount = alarm.snoozeCount + 1
                
                // Calculate snooze trigger time (now + snoozeDurationMinutes)
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, alarm.snoozeDurationMinutes)
                }
                
                val snoozedAlarm = alarm.copy(
                    hour = cal.get(Calendar.HOUR_OF_DAY),
                    minute = cal.get(Calendar.MINUTE),
                    snoozeCount = updatedSnoozeCount
                )

                alarmDao.updateAlarm(snoozedAlarm)
                AndroidAlarmScheduler(context).schedule(snoozedAlarm)

                _uiState.update { it.copy(isSnoozed = true, isDismissed = true) }
                onSuccess()
            }
        }
    }
}
