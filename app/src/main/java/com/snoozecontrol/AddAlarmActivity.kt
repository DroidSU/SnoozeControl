package com.snoozecontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snoozecontrol.model.AlarmItem
import com.snoozecontrol.model.ChallengeType
import com.snoozecontrol.ui.AddEditAlarmScreen
import com.snoozecontrol.ui.BarcodeRegistrationScreen
import com.snoozecontrol.ui.theme.SnoozeControlTheme
import com.snoozecontrol.viewmodel.AddEditAlarmViewModel

class AddAlarmActivity : ComponentActivity() {
    private val viewModel: AddEditAlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val alarmHour = intent.getIntExtra(EXTRA_ALARM_HOUR, -1)
        val alarmMinute = intent.getIntExtra(EXTRA_ALARM_MINUTE, -1)
        val alarmChallengeStr = intent.getStringExtra(EXTRA_ALARM_CHALLENGE)
        val alarmBarcode = intent.getStringExtra(EXTRA_ALARM_BARCODE)
        val alarmRepeatDays = intent.getStringExtra(EXTRA_ALARM_REPEAT_DAYS) ?: ""
        val alarmSnoozeDuration = intent.getIntExtra(EXTRA_ALARM_SNOOZE_DURATION, 5)

        val initialAlarm = if (alarmId != -1 && alarmHour != -1) {
            AlarmItem(
                id = alarmId,
                hour = alarmHour,
                minute = alarmMinute,
                isEnabled = true,
                challengeType = try {
                    ChallengeType.valueOf(alarmChallengeStr ?: "MATH")
                } catch (e: Exception) {
                    ChallengeType.MATH
                },
                targetBarcode = alarmBarcode,
                repeatDays = alarmRepeatDays,
                snoozeDurationMinutes = alarmSnoozeDuration
            )
        } else null

        viewModel.initAlarm(initialAlarm)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val navController = rememberNavController()

            SnoozeControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "add_edit_alarm"
                    ) {
                        composable("add_edit_alarm") { backStackEntry ->
                            val barcodeResult by backStackEntry.savedStateHandle
                                .getStateFlow<String?>("barcode_result", null).collectAsState()

                            LaunchedEffect(barcodeResult) {
                                if (barcodeResult != null) {
                                    viewModel.onBarcodeResult(barcodeResult)
                                    backStackEntry.savedStateHandle.remove<String>("barcode_result")
                                }
                            }

                            AddEditAlarmScreen(
                                uiState = uiState,
                                onHourChange = viewModel::onHourChange,
                                onMinuteChange = viewModel::onMinuteChange,
                                onAmPmChange = viewModel::onAmPmChange,
                                onChallengeTypeChange = viewModel::onChallengeTypeChange,
                                onDaysChange = viewModel::onDaysChange,
                                onSnoozeDurationChange = viewModel::onSnoozeDurationChange,
                                onSave = {
                                    viewModel.saveAlarm(this@AddAlarmActivity) {
                                        setResult(RESULT_OK)
                                        finish()
                                    }
                                },
                                onBack = {
                                    setResult(RESULT_CANCELED)
                                    finish()
                                },
                                onRegisterBarcodeClick = {
                                    navController.navigate("register_barcode")
                                }
                            )
                        }

                        composable("register_barcode") {
                            BarcodeRegistrationScreen(
                                onBarcodeScanned = { barcode ->
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("barcode_result", barcode)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
        const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
        const val EXTRA_ALARM_CHALLENGE = "extra_alarm_challenge"
        const val EXTRA_ALARM_BARCODE = "extra_alarm_barcode"
        const val EXTRA_ALARM_REPEAT_DAYS = "extra_alarm_repeat_days"
        const val EXTRA_ALARM_SNOOZE_DURATION = "extra_alarm_snooze_duration"

        fun createIntent(
            context: Context,
            alarm: AlarmItem? = null
        ): Intent {
            return Intent(context, AddAlarmActivity::class.java).apply {
                if (alarm != null) {
                    putExtra(EXTRA_ALARM_ID, alarm.id)
                    putExtra(EXTRA_ALARM_HOUR, alarm.hour)
                    putExtra(EXTRA_ALARM_MINUTE, alarm.minute)
                    putExtra(EXTRA_ALARM_CHALLENGE, alarm.challengeType.name)
                    putExtra(EXTRA_ALARM_BARCODE, alarm.targetBarcode)
                    putExtra(EXTRA_ALARM_REPEAT_DAYS, alarm.repeatDays)
                    putExtra(EXTRA_ALARM_SNOOZE_DURATION, alarm.snoozeDurationMinutes)
                } else {
                    putExtra(EXTRA_ALARM_ID, -1)
                }
            }
        }
    }
}
