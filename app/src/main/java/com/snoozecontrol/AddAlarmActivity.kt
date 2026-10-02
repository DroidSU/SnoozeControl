package com.snoozecontrol

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.snoozecontrol.model.MathDifficulty
import com.snoozecontrol.ui.AddEditAlarmScreen
import com.snoozecontrol.ui.BarcodeRegistrationScreen
import com.snoozecontrol.ui.theme.SnoozeControlTheme
import com.snoozecontrol.util.PermissionManager
import com.snoozecontrol.viewmodel.AddEditAlarmViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddAlarmActivity : ComponentActivity() {
    private val viewModel: AddEditAlarmViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.saveAlarm(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private var pendingNavigateToBarcode = false
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingNavigateToBarcode = false
        }
    }

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val title = if (uri != null) {
                RingtoneManager.getRingtone(this, uri)?.getTitle(this) ?: "Selected Sound"
            } else {
                "Default Alarm Sound"
            }
            viewModel.onRingtoneSelected(uri?.toString(), title)
        }
    }

    private fun launchRingtonePicker(currentUriStr: String?) {
        val currentUri = currentUriStr?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        ringtonePickerLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val alarmHour = intent.getIntExtra(EXTRA_ALARM_HOUR, -1)
        val alarmMinute = intent.getIntExtra(EXTRA_ALARM_MINUTE, -1)
        val alarmChallengeStr = intent.getStringExtra(EXTRA_ALARM_CHALLENGE)
        val alarmDifficultyStr = intent.getStringExtra(EXTRA_ALARM_DIFFICULTY)
        val alarmBarcode = intent.getStringExtra(EXTRA_ALARM_BARCODE)
        val alarmRingtoneUri = intent.getStringExtra(EXTRA_ALARM_RINGTONE_URI)
        val alarmRingtoneTitle =
            intent.getStringExtra(EXTRA_ALARM_RINGTONE_TITLE) ?: "Default Alarm Sound"
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
                mathDifficulty = try {
                    MathDifficulty.valueOf(alarmDifficultyStr ?: "MEDIUM")
                } catch (e: Exception) {
                    MathDifficulty.MEDIUM
                },
                targetBarcode = alarmBarcode,
                ringtoneUri = alarmRingtoneUri,
                ringtoneTitle = alarmRingtoneTitle,
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
                                onMathDifficultyChange = viewModel::onMathDifficultyChange,
                                onSelectRingtoneClick = { launchRingtonePicker(uiState.ringtoneUri) },
                                onDaysChange = viewModel::onDaysChange,
                                onSnoozeDurationChange = viewModel::onSnoozeDurationChange,
                                onBedtimeReminderToggle = viewModel::onBedtimeReminderToggle,
                                onNextStep = viewModel::nextStep,
                                onPrevStep = viewModel::prevStep,
                                onSetStep = viewModel::setStep,
                                onSave = {
                                    if (!PermissionManager.hasNotificationPermission(this@AddAlarmActivity)) {
                                        PermissionManager.getNotificationPermission()?.let { perm ->
                                            notificationPermissionLauncher.launch(perm)
                                        } ?: viewModel.saveAlarm(this@AddAlarmActivity) {
                                            setResult(RESULT_OK)
                                            finish()
                                        }
                                    } else {
                                        viewModel.saveAlarm(this@AddAlarmActivity) {
                                            setResult(RESULT_OK)
                                            finish()
                                        }
                                    }
                                },
                                onBack = {
                                    if (uiState.currentStep > 1) {
                                        viewModel.prevStep()
                                    } else {
                                        setResult(RESULT_CANCELED)
                                        finish()
                                    }
                                },
                                onRegisterBarcodeClick = {
                                    if (!PermissionManager.hasCameraPermission(this@AddAlarmActivity)) {
                                        pendingNavigateToBarcode = true
                                        cameraPermissionLauncher.launch(PermissionManager.getCameraPermission())
                                    } else {
                                        navController.navigate("register_barcode")
                                    }
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
        const val EXTRA_ALARM_DIFFICULTY = "extra_alarm_difficulty"
        const val EXTRA_ALARM_BARCODE = "extra_alarm_barcode"
        const val EXTRA_ALARM_RINGTONE_URI = "extra_alarm_ringtone_uri"
        const val EXTRA_ALARM_RINGTONE_TITLE = "extra_alarm_ringtone_title"
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
                    putExtra(EXTRA_ALARM_DIFFICULTY, alarm.mathDifficulty.name)
                    putExtra(EXTRA_ALARM_BARCODE, alarm.targetBarcode)
                    putExtra(EXTRA_ALARM_RINGTONE_URI, alarm.ringtoneUri)
                    putExtra(EXTRA_ALARM_RINGTONE_TITLE, alarm.ringtoneTitle)
                    putExtra(EXTRA_ALARM_REPEAT_DAYS, alarm.repeatDays)
                    putExtra(EXTRA_ALARM_SNOOZE_DURATION, alarm.snoozeDurationMinutes)
                } else {
                    putExtra(EXTRA_ALARM_ID, -1)
                }
            }
        }
    }
}
