package com.snoozecontrol

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snoozecontrol.ui.AlarmScreen
import com.snoozecontrol.ui.theme.SnoozeControlTheme
import com.snoozecontrol.viewmodel.AlarmViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockScreenFlags()
        enableEdgeToEdge()

        requestAppPermissions()

        handleIntent(intent)

        setContent {
            val alarms by viewModel.alarms.collectAsState()
            val nextAlarm by viewModel.nextAlarm.collectAsState()
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            SnoozeControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val snackbarHostState = remember { SnackbarHostState() }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = "alarm_list",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("alarm_list") {
                                AlarmScreen(
                                    alarms = alarms,
                                    nextAlarm = nextAlarm,
                                    onAddClick = {
                                        context.startActivity(
                                            AddAlarmActivity.createIntent(context, null)
                                        )
                                    },
                                    onEditClick = { id ->
                                        val alarm = alarms.find { it.id == id }
                                        context.startActivity(
                                            AddAlarmActivity.createIntent(context, alarm)
                                        )
                                    },
                                    onToggleAlarm = { id ->
                                        viewModel.toggleAlarm(
                                            this@MainActivity,
                                            id
                                        )
                                    },
                                    onDeleteAlarm = { alarm -> 
                                        viewModel.deleteAlarm(this@MainActivity, alarm)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = getString(R.string.snackbar_alarm_deleted),
                                                actionLabel = getString(R.string.snackbar_undo),
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete(this@MainActivity)
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
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
            val alarmId = intent.getIntExtra("ALARM_ID", -1)
            startActivity(AlarmDismissActivity.createIntent(this, alarmId))
        }
    }

    private fun requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 102)
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
}
