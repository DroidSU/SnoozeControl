package com.snoozecontrol

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.snoozecontrol.service.AlarmService
import com.snoozecontrol.ui.AlarmDismissScreen
import com.snoozecontrol.ui.MorningDashboardScreen
import com.snoozecontrol.ui.theme.SnoozeControlTheme
import com.snoozecontrol.viewmodel.AlarmDismissViewModel

class AlarmDismissActivity : ComponentActivity() {
    private val viewModel: AlarmDismissViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockScreenFlags()
        enableEdgeToEdge()

        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        viewModel.loadDismissChallenge(alarmId)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val weatherInfo by viewModel.weatherInfo.collectAsState()

            LaunchedEffect(uiState.isDismissed) {
                if (uiState.isDismissed) {
                    stopAlarmService()
                    if (uiState.isSnoozed) {
                        finish()
                    }
                }
            }

            SnoozeControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (uiState.showMorningDashboard) {
                        MorningDashboardScreen(
                            weatherInfo = weatherInfo,
                            isWeatherLoading = uiState.isWeatherLoading,
                            quote = uiState.quote,
                            onStartDayClick = { finish() }
                        )
                    } else {
                        AlarmDismissScreen(
                            isSuccess = uiState.isDismissed,
                            challengeType = uiState.challengeType,
                            equation = uiState.equation,
                            answerInput = uiState.answerInput,
                            errorMessage = uiState.errorMessage,
                            canSnooze = uiState.canSnooze,
                            snoozeDurationMinutes = uiState.snoozeDurationMinutes,
                            remainingSnoozes = uiState.remainingSnoozes,
                            onAnswerChange = viewModel::onAnswerChange,
                            onBarcodeScanned = { scannedBarcode ->
                                viewModel.onBarcodeScanned(scannedBarcode) {}
                            },
                            onDismissClick = {
                                viewModel.checkMathAnswer {
                                }
                            },
                            onSnoozeClick = {
                                viewModel.snoozeAlarm(this@AlarmDismissActivity) {}
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true // Block physical volume key muting
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true // Block physical volume key muting
            else -> super.onKeyUp(keyCode, event)
        }
    }

    private fun stopAlarmService() {
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"

        fun createIntent(context: Context, alarmId: Int): Intent {
            return Intent(context, AlarmDismissActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
        }
    }
}
