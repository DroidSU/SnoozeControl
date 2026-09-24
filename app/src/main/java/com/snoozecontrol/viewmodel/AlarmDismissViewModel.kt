package com.snoozecontrol.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snoozecontrol.R
import com.snoozecontrol.data.AlarmDatabase
import com.snoozecontrol.model.ChallengeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmDismissUiState(
    val alarmId: Int = -1,
    val challengeType: ChallengeType = ChallengeType.NONE,
    val equation: String = "",
    val correctAnswer: Int = 0,
    val answerInput: String = "",
    val targetBarcode: String? = null,
    val errorMessage: String? = null,
    val isDismissed: Boolean = false
)

class AlarmDismissViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()

    private val _uiState = MutableStateFlow(AlarmDismissUiState())
    val uiState: StateFlow<AlarmDismissUiState> = _uiState.asStateFlow()

    fun loadDismissChallenge(alarmId: Int) {
        viewModelScope.launch {
            val alarm = if (alarmId != -1) alarmDao.getAlarmById(alarmId) else null
            if (alarm != null) {
                when (alarm.challengeType) {
                    ChallengeType.MATH -> generateMathProblem(alarmId)
                    ChallengeType.BARCODE -> {
                        _uiState.value = AlarmDismissUiState(
                            alarmId = alarmId,
                            challengeType = ChallengeType.BARCODE,
                            targetBarcode = alarm.targetBarcode
                        )
                    }

                    ChallengeType.NONE -> {
                        _uiState.value = AlarmDismissUiState(
                            alarmId = alarmId,
                            challengeType = ChallengeType.NONE
                        )
                    }
                }
            } else {
                generateMathProblem(alarmId)
            }
        }
    }

    private fun generateMathProblem(alarmId: Int) {
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
            correctAnswer = answer
        )
    }

    fun onAnswerChange(newInput: String) {
        _uiState.update { it.copy(answerInput = newInput, errorMessage = null) }
    }

    fun checkMathAnswer(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val userTypedAnswer = currentState.answerInput.trim().toIntOrNull()

        if (userTypedAnswer == currentState.correctAnswer) {
            _uiState.update { it.copy(isDismissed = true) }
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
                _uiState.update { it.copy(isDismissed = true) }
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
}
