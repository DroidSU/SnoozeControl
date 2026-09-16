package com.snoozecontrol

import android.content.Intent
import android.os.Bundle
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
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import android.app.KeyguardManager
import android.content.Context
import com.snoozecontrol.service.AlarmService
import com.snoozecontrol.ui.AlarmDismissScreen
import com.snoozecontrol.ui.AlarmScreen
import com.snoozecontrol.ui.AlarmViewModel
import com.snoozecontrol.ui.theme.SnoozeControlTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupLockScreenFlags()
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        handleIntent(intent)

        setContent {
            val alarms by viewModel.alarms.collectAsState()
            val dismissState by viewModel.dismissState.collectAsState()

            SnoozeControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (dismissState.equation.isNotEmpty()) {
                        AlarmDismissScreen(
                            equation = dismissState.equation,
                            answerInput = dismissState.input,
                            errorMessage = dismissState.error,
                            onAnswerChange = viewModel::onAnswerChange,
                            onDismissClick = {
                                viewModel.checkAnswer {
                                    stopService(Intent(this@MainActivity, AlarmService::class.java))
                                }
                            }
                        )
                    } else {
                        AlarmScreen(
                            alarms = alarms,
                            onAddAlarm = { hour, minute ->
                                viewModel.addAlarm(this@MainActivity, hour, minute)
                            },
                            onToggleAlarm = { id ->
                                viewModel.toggleAlarm(this@MainActivity, id)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("ALARM_TRIGGERED", false) == true) {
            viewModel.generateNewMathProblem()
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
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
}
