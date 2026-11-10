package com.snoozecontrol.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snoozecontrol.R
import com.snoozecontrol.data.AlarmDao
import com.snoozecontrol.data.billing.BillingRepository
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
    val quote: Quote = QuoteProvider.getTodayQuote(),
    val shakeProgress: Float = 0f,
    val currentShakes: Int = 0,
    val requiredShakes: Int = 35,
    val failedAttempts: Int = 0,
    val isProUser: Boolean = false,
    val isDebugProEnabled: Boolean = false
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
    private val scheduler: AlarmScheduler,
    private val billingRepository: BillingRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AlarmDismissUiState())
    val uiState: StateFlow<AlarmDismissUiState> = _uiState.asStateFlow()

    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo = _weatherInfo.asStateFlow()

    init {
        getCurrentWeather()
        observeBillingStatus()
    }

    private fun observeBillingStatus() {
        viewModelScope.launch {
            billingRepository.isProUser.collect { isPro ->
                _uiState.update {
                    it.copy(
                        isProUser = isPro,
                        isDebugProEnabled = billingRepository.isDebugProEnabled()
                    )
                }
            }
        }
    }

    fun toggleDebugProOverride() {
        val next = billingRepository.toggleDebugProOverride()
        _uiState.update {
            it.copy(
                isDebugProEnabled = next,
                isProUser = next || billingRepository.isProUser.value
            )
        }
    }

    fun launchBillingFlow(activity: Activity): Boolean {
        return billingRepository.launchBillingFlow(activity)
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
        difficulty: MathDifficulty = MathDifficulty.EASY
    ) {
        val (equation, answer) = when (difficulty) {
            MathDifficulty.EASY -> {
                // Easy: Two-digit simple mathematics (e.g. 2 * 10, 29 * 19, 45 + 38, 92 - 47)
                val problemType = (1..3).random()
                when (problemType) {
                    1 -> {
                        // 2-digit multiplication (e.g. 2 * 10, 29 * 19)
                        val num1 = if ((1..2).random() == 1) (2..9).random() else (10..29).random()
                        val num2 = (10..20).random()
                        "$num1 × $num2" to (num1 * num2)
                    }

                    2 -> {
                        // 2-digit addition
                        val num1 = (10..99).random()
                        val num2 = (10..99).random()
                        "$num1 + $num2" to (num1 + num2)
                    }

                    else -> {
                        // 2-digit subtraction
                        val num1 = (30..99).random()
                        val num2 = (10..num1).random()
                        "$num1 - $num2" to (num1 - num2)
                    }
                }
            }

            MathDifficulty.GENIUS -> {
                // Genius: 3-digit harder problems (e.g. 354 + 478, 832 - 475, 125 * 8, 124 * 6 + 45)
                val problemType = (1..3).random()
                when (problemType) {
                    1 -> {
                        // 3-digit addition
                        val num1 = (100..999).random()
                        val num2 = (100..999).random()
                        "$num1 + $num2" to (num1 + num2)
                    }

                    2 -> {
                        // 3-digit subtraction
                        val num1 = (200..999).random()
                        val num2 = (100..num1).random()
                        "$num1 - $num2" to (num1 - num2)
                    }

                    else -> {
                        // Multi-step with 3-digit or 3-digit multiplication
                        val isMultiStep = (1..2).random() == 1
                        if (isMultiStep) {
                            val num1 = (10..35).random()
                            val num2 = (10..25).random()
                            val num3 = (50..300).random()
                            val op = listOf("+", "-").random()
                            val prod = num1 * num2
                            if (op == "+") "$num1 × $num2 + $num3" to (prod + num3)
                            else {
                                val maxVal = maxOf(prod, num3)
                                val minVal = minOf(prod, num3)
                                "$maxVal - $minVal" to (maxVal - minVal)
                            }
                        } else {
                            val num1 = (100..500).random()
                            val num2 = (3..9).random()
                            "$num1 × $num2" to (num1 * num2)
                        }
                    }
                }
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
                snoozeCount = currentSnooze,
                failedAttempts = 0
            )
        }
    }

    fun onAnswerChange(newInput: String) {
        _uiState.update { it.copy(answerInput = newInput, errorMessage = null) }
    }

    fun onDismissClick(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value
        when (currentState.challengeType) {
            ChallengeType.NONE -> {
                resetSnoozeCountAndDismiss(currentState.alarmId)
                onSuccess()
            }

            ChallengeType.SHAKE -> {
                if (currentState.shakeProgress >= 1f) {
                    resetSnoozeCountAndDismiss(currentState.alarmId)
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = getApplication<Application>().getString(R.string.shake_instruction_error)
                        )
                    }
                }
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

    fun onPhoneShaken(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value
        if (currentState.isDismissed || currentState.challengeType != ChallengeType.SHAKE) return

        val newShakes = currentState.currentShakes + 1
        val newProgress = (newShakes / currentState.requiredShakes.toFloat()).coerceIn(0f, 1f)

        _uiState.update {
            it.copy(
                currentShakes = newShakes,
                shakeProgress = newProgress,
                errorMessage = null
            )
        }

        if (newProgress >= 1f) {
            resetSnoozeCountAndDismiss(currentState.alarmId)
            onSuccess()
        }
    }

    fun checkMathAnswer(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value
        if (currentState.challengeType == ChallengeType.MATH) {
            val userEntered = currentState.answerInput.trim().toIntOrNull()
            if (userEntered != null && userEntered == currentState.correctAnswer) {
                resetSnoozeCountAndDismiss(currentState.alarmId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = getApplication<Application>().getString(R.string.error_incorrect_answer),
                        failedAttempts = it.failedAttempts + 1
                    )
                }
            }
        } else {
            onDismissClick(onSuccess)
        }
    }

    fun emergencyBypass(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value
        resetSnoozeCountAndDismiss(currentState.alarmId)
        onSuccess()
    }

    fun switchToShakeEmergencyFallback() {
        _uiState.update {
            it.copy(
                challengeType = ChallengeType.SHAKE,
                shakeProgress = 0f,
                currentShakes = 0,
                requiredShakes = 35,
                errorMessage = null
            )
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
                    showMorningDashboard = false,
                    isWeatherLoading = _weatherInfo.value == null
                )
            }
            if (_weatherInfo.value == null) {
                getCurrentWeather()
            }

            kotlinx.coroutines.delay(1500)
            _uiState.update {
                it.copy(showMorningDashboard = true)
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
