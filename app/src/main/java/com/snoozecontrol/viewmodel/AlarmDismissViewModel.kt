package com.snoozecontrol.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snoozecontrol.R
import com.snoozecontrol.data.AlarmDao
import com.snoozecontrol.model.ChallengeType
import com.snoozecontrol.model.MathDifficulty
import com.snoozecontrol.scheduler.AlarmScheduler
import com.snoozecontrol.util.Quote
import com.snoozecontrol.util.QuoteProvider
import com.snoozecontrol.util.UpcomingAlarmNotificationManager
import com.snoozecontrol.util.WeatherInfo
import com.snoozecontrol.util.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

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
    val userName: String = "Sujoy",
    val quote: Quote = QuoteProvider.getTodayQuote()
) {
    val canSnooze: Boolean
        get() = snoozeCount < maxSnoozeCount && maxSnoozeCount > 0

    val remainingSnoozes: Int
        get() = (maxSnoozeCount - snoozeCount).coerceAtLeast(0)
}

@HiltViewModel
class AlarmDismissViewModel @Inject constructor(
    application: Application,
    private val alarmDao: AlarmDao,
    private val scheduler: AlarmScheduler
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AlarmDismissUiState())
    val uiState: StateFlow<AlarmDismissUiState> = _uiState.asStateFlow()

    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo = _weatherInfo.asStateFlow()

    init {
        getCurrentWeather()
    }

    fun getCurrentWeather(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherLoading = true) }
            _weatherInfo.value = WeatherRepository.fetchCurrentWeather(
                getApplication(),
                forceRefresh = forceRefresh
            )
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
                    ChallengeType.MATH -> generateMathProblem(
                        alarmId,
                        snoozeDur,
                        maxSnooze,
                        currentSnooze,
                        alarm.mathDifficulty
                    )
                    ChallengeType.BARCODE -> {
                        _uiState.update {
                            it.copy(
                                alarmId = alarmId,
                                challengeType = ChallengeType.BARCODE,
                                targetBarcode = alarm.targetBarcode,
                                snoozeDurationMinutes = snoozeDur,
                                maxSnoozeCount = maxSnooze,
                                snoozeCount = currentSnooze
                            )
                        }
                    }

                    ChallengeType.SHAKE, ChallengeType.NONE -> {
                        _uiState.update {
                            it.copy(
                                alarmId = alarmId,
                                challengeType = alarm.challengeType,
                                snoozeDurationMinutes = snoozeDur,
                                maxSnoozeCount = maxSnooze,
                                snoozeCount = currentSnooze
                            )
                        }
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
        currentSnooze: Int,
        difficulty: MathDifficulty = MathDifficulty.MEDIUM
    ) {
        val (equation, answer) = when (difficulty) {
            MathDifficulty.EASY -> {
                val num1 = (1..10).random()
                val num2 = (1..10).random()
                val op = listOf("+", "-").random()
                if (op == "+") "$num1 + $num2" to (num1 + num2)
                else {
                    val max = maxOf(num1, num2)
                    val min = minOf(num1, num2)
                    "$max - $min" to (max - min)
                }
            }

            MathDifficulty.MEDIUM -> {
                val num1 = (10..50).random()
                val num2 = (2..12).random()
                val op = listOf("+", "-", "*").random()
                when (op) {
                    "+" -> "$num1 + $num2" to (num1 + num2)
                    "-" -> {
                        val n2 = (1..num1).random()
                        "$num1 - $n2" to (num1 - n2)
                    }

                    else -> "$num1 * $num2" to (num1 * num2)
                }
            }

            MathDifficulty.HARD -> {
                val num1 = (12..30).random()
                val num2 = (3..15).random()
                val num3 = (5..25).random()
                val op = listOf("+", "-").random()
                val prod = num1 * num2
                if (op == "+") "$num1 * $num2 + $num3" to (prod + num3)
                else "$num1 * $num2 - $num3" to (prod - num3)
            }
        }

        _uiState.update {
            it.copy(
                alarmId = alarmId,
                challengeType = ChallengeType.MATH,
                equation = equation,
                correctAnswer = answer,
                snoozeDurationMinutes = snoozeDur,
                maxSnoozeCount = maxSnooze,
                snoozeCount = currentSnooze
            )
        }
    }

    fun onAnswerChange(newInput: String) {
        _uiState.update { it.copy(answerInput = newInput, errorMessage = null) }
    }

    fun onDismissClick(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value
        when (currentState.challengeType) {
            ChallengeType.NONE, ChallengeType.SHAKE -> {
                resetSnoozeCountAndDismiss(currentState.alarmId)
                onSuccess()
            }

            ChallengeType.MATH -> checkMathAnswer(onSuccess)
            ChallengeType.BARCODE -> {
                _uiState.update {
                    it.copy(
                        errorMessage = getApplication<Application>().getString(R.string.scan_barcode_instruction)
                    )
                }
            }
        }
    }

    fun checkMathAnswer(onSuccess: () -> Unit) {
        onDismissClick(onSuccess)
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
                        snoozedUntilMillis = null,
                        isEnabled = if (isOnce) false else alarm.isEnabled
                    )
                    alarmDao.updateAlarm(updatedAlarm)
                    if (isOnce) {
                        scheduler.cancel(updatedAlarm)
                    } else {
                        scheduler.schedule(updatedAlarm)
                    }
                }
            }
            UpcomingAlarmNotificationManager.refreshUpcomingNotification(getApplication())

            _uiState.update {
                it.copy(
                    isDismissed = true,
                    showMorningDashboard = true,
                    isWeatherLoading = _weatherInfo.value == null
                )
            }
            if (_weatherInfo.value == null) {
                getCurrentWeather()
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
                    snoozeCount = updatedSnoozeCount,
                    snoozedUntilMillis = cal.timeInMillis
                )

                alarmDao.updateAlarm(snoozedAlarm)
                scheduler.schedule(snoozedAlarm)
                UpcomingAlarmNotificationManager.refreshUpcomingNotification(getApplication())

                _uiState.update { it.copy(isSnoozed = true, isDismissed = true) }
                onSuccess()
            }
        }
    }
}
