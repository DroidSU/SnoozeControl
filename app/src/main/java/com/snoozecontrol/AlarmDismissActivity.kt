package com.snoozecontrol

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.snoozecontrol.service.AlarmService
import com.snoozecontrol.ui.AlarmDismissScreen
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

            SnoozeControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmDismissScreen(
                        challengeType = uiState.challengeType,
                        equation = uiState.equation,
                        answerInput = uiState.answerInput,
                        errorMessage = uiState.errorMessage,
                        onAnswerChange = viewModel::onAnswerChange,
                        onBarcodeScanned = { scannedBarcode ->
                            viewModel.onBarcodeScanned(scannedBarcode) {
                                dismissServiceAndFinish()
                            }
                        },
                        onDismissClick = {
                            viewModel.checkMathAnswer {
                                dismissServiceAndFinish()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun dismissServiceAndFinish() {
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        finish()
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
