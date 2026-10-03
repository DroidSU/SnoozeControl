package com.snoozecontrol.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.R
import com.snoozecontrol.model.ChallengeType
import com.snoozecontrol.model.MathDifficulty
import com.snoozecontrol.ui.theme.SnoozeControlTheme
import com.snoozecontrol.util.Utils
import com.snoozecontrol.viewmodel.AddEditAlarmUiState
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    uiState: AddEditAlarmUiState,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit,
    onChallengeTypeChange: (ChallengeType) -> Unit,
    onMathDifficultyChange: (MathDifficulty) -> Unit = {},
    onSelectRingtoneClick: () -> Unit = {},
    onDaysChange: (Set<Int>) -> Unit,
    onSnoozeDurationChange: (Int) -> Unit = {},
    onBedtimeReminderToggle: (Boolean) -> Unit = {},
    onNextStep: () -> Unit = {},
    onPrevStep: () -> Unit = {},
    onSetStep: (Int) -> Unit = {},
    onSave: () -> Unit,
    onBack: () -> Unit,
    onRegisterBarcodeClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.set_alarm_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Step ${uiState.currentStep} of 3",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.currentStep > 1) {
                        OutlinedButton(
                            onClick = onPrevStep,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                "Back",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (uiState.currentStep < 3) {
                                onNextStep()
                            } else {
                                onSave()
                            }
                        },
                        modifier = Modifier
                            .weight(if (uiState.currentStep > 1) 1f else 2f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = if (uiState.currentStep == 2) uiState.isStep2Valid else true,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = if (uiState.currentStep < 3) "Continue" else "Create Alarm",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        if (uiState.currentStep < 3) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3 DOTS STEP INDICATOR
                ThreeDotsStepIndicator(currentStep = uiState.currentStep)

                // STEP CONTENT WIZARD
                when (uiState.currentStep) {
                    1 -> StepOneTime(
                        hour12 = uiState.hour12,
                        minute = uiState.minute,
                        isAm = uiState.isAm,
                        onHourChange = onHourChange,
                        onMinuteChange = onMinuteChange,
                        onAmPmChange = onAmPmChange
                    )

                    2 -> StepTwoWakeMethod(
                        challengeType = uiState.challengeType,
                        mathDifficulty = uiState.mathDifficulty,
                        targetBarcode = uiState.targetBarcode,
                        onChallengeTypeChange = onChallengeTypeChange,
                        onMathDifficultyChange = onMathDifficultyChange,
                        onRegisterBarcodeClick = onRegisterBarcodeClick
                    )

                    3 -> StepThreeSchedule(
                        ringtoneTitle = uiState.ringtoneTitle,
                        onSelectRingtoneClick = onSelectRingtoneClick,
                        snoozeDurationMinutes = uiState.snoozeDurationMinutes,
                        onSnoozeDurationChange = onSnoozeDurationChange,
                        isBedtimeReminderEnabled = uiState.isBedtimeReminderEnabled,
                        onBedtimeReminderToggle = onBedtimeReminderToggle,
                        selectedDays = uiState.selectedDays,
                        onDaysChange = onDaysChange
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ==========================================
// 3 DOTS STEP INDICATOR
// ==========================================
@Composable
fun ThreeDotsStepIndicator(currentStep: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        (1..3).forEach { step ->
            val isActive = currentStep == step
            val isPassed = currentStep > step
            val dotWidth by animateDpAsState(
                targetValue = if (isActive) 24.dp else 8.dp,
                label = "dotWidth"
            )
            val dotColor by animateColorAsState(
                targetValue = when {
                    isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                },
                label = "dotColor"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

// ==========================================
// STEP 1 — CHOOSE THE TIME
// ==========================================
@Composable
fun StepOneTime(
    hour12: Int,
    minute: Int,
    isAm: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Alarm Time",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        TimePickerCard(
            hour12 = hour12,
            minute = minute,
            isAm = isAm,
            onHourChange = onHourChange,
            onMinuteChange = onMinuteChange,
            onAmPmChange = onAmPmChange
        )
    }
}

// ==========================================
// STEP 2 — CHOOSE HOW TO WAKE UP
// ==========================================
@Composable
fun StepTwoWakeMethod(
    challengeType: ChallengeType,
    mathDifficulty: MathDifficulty,
    targetBarcode: String?,
    onChallengeTypeChange: (ChallengeType) -> Unit,
    onMathDifficultyChange: (MathDifficulty) -> Unit,
    onRegisterBarcodeClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wake-Up Challenge",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Math Genius Option
        SelectableChallengeCard(
            title = stringResource(R.string.math_challenge_title),
            description = stringResource(R.string.math_challenge_desc),
            icon = Icons.Default.Calculate,
            isSelected = challengeType == ChallengeType.MATH,
            onClick = { onChallengeTypeChange(ChallengeType.MATH) }
        ) {
            AnimatedVisibility(
                visible = challengeType == ChallengeType.MATH,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Difficulty",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MathDifficulty.entries.forEach { diff ->
                                val isDiffSelected = mathDifficulty == diff
                                val bg by animateColorAsState(
                                    if (isDiffSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    label = "diffBg"
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg)
                                        .clickable { onMathDifficultyChange(diff) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = diff.displayName,
                                        fontWeight = if (isDiffSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        color = if (isDiffSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Barcode Scan Option
        SelectableChallengeCard(
            title = stringResource(R.string.barcode_challenge_title),
            description = if (targetBarcode != null) "Barcode registered successfully!" else stringResource(
                R.string.barcode_challenge_desc
            ),
            icon = Icons.Default.QrCodeScanner,
            isSelected = challengeType == ChallengeType.BARCODE,
            onClick = { onChallengeTypeChange(ChallengeType.BARCODE) }
        ) {
            AnimatedVisibility(
                visible = challengeType == ChallengeType.BARCODE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Button(
                        onClick = onRegisterBarcodeClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (targetBarcode != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                            contentColor = if (targetBarcode != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (targetBarcode != null) "Rescan / Change Barcode" else "Set up barcode",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Shake Phone Option
        SelectableChallengeCard(
            title = "Shake Phone",
            description = "Shake to dismiss the alarm",
            icon = Icons.Default.Vibration,
            isSelected = challengeType == ChallengeType.SHAKE,
            onClick = { onChallengeTypeChange(ChallengeType.SHAKE) }
        ) {
            AnimatedVisibility(
                visible = challengeType == ChallengeType.SHAKE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "Shake your phone vigorously to dismiss the alarm when it rings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SelectableChallengeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
            alpha = 0.2f
        ),
        label = "borderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        label = "containerColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        border = BorderStroke(if (isSelected) 2.dp else 0.5.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                }
            }

            if (content != null) {
                content()
            }
        }
    }
}

// ==========================================
// STEP 3 — SNOOZE + REPEAT + SOUND
// ==========================================
@Composable
fun StepThreeSchedule(
    ringtoneTitle: String,
    onSelectRingtoneClick: () -> Unit,
    snoozeDurationMinutes: Int,
    onSnoozeDurationChange: (Int) -> Unit,
    isBedtimeReminderEnabled: Boolean,
    onBedtimeReminderToggle: (Boolean) -> Unit,
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Schedule & Repeat",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. REPEAT SCHEDULE CARD
        RepeatScheduleCard(selectedDays = selectedDays, onDaysChange = onDaysChange)

        Spacer(modifier = Modifier.height(16.dp))

        // 2. SNOOZE SETTINGS CARD
        SnoozeSettingsCard(
            snoozeDurationMinutes = snoozeDurationMinutes,
            onSnoozeDurationChange = onSnoozeDurationChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. SOUND SELECTION CARD
        SoundSelectionCard(
            ringtoneTitle = ringtoneTitle,
            onSelectRingtoneClick = onSelectRingtoneClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. BEDTIME REMINDER CARD
        BedtimeReminderCard(
            isEnabled = isBedtimeReminderEnabled,
            onToggle = onBedtimeReminderToggle
        )
    }
}

@Composable
fun SoundSelectionCard(
    ringtoneTitle: String,
    onSelectRingtoneClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        onClick = onSelectRingtoneClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alarm Sound",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ringtoneTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Select Ringtone",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
        }
    }
}

// TIME PICKER CARD COMPONENT
@Composable
fun TimePickerCard(
    hour12: Int,
    minute: Int,
    isAm: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Alarm Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Time Display
            val formattedTime = remember(hour12, minute) {
                Utils.formatTimeDigits(hour12, minute)
            }
            val amPmText = if (isAm) "AM" else "PM"
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPmText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Compact Wheel Selector Body
            CustomDrumTimePicker(
                hour12 = hour12,
                minute = minute,
                onHourChange = onHourChange,
                onMinuteChange = onMinuteChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // AM / PM Segmented Switch Pill
            AmPmSegmentedControl(isAm = isAm, onAmPmChange = onAmPmChange)
        }
    }
}

// AM / PM SEGMENTED SWITCH PILL
@Composable
fun AmPmSegmentedControl(
    isAm: Boolean,
    onAmPmChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val amBg by animateColorAsState(
                if (isAm) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "amBg"
            )
            val pmBg by animateColorAsState(
                if (!isAm) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "pmBg"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(amBg)
                    .clickable { onAmPmChange(true) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = if (isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(pmBg)
                    .clickable { onAmPmChange(false) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = if (!isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// CUSTOM WHEEL / DRUM TIME PICKER
@Composable
fun CustomDrumTimePicker(
    hour12: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour Wheel (1..12)
        WheelColumn(
            range = 1..12,
            selectedValue = hour12,
            onValueChange = onHourChange,
            format = { String.format(Locale.getDefault(), "%02d", it) },
            modifier = Modifier.weight(1f)
        )

        Text(
            text = ":",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Minute Wheel (0..59)
        WheelColumn(
            range = 0..59,
            selectedValue = minute,
            onValueChange = onMinuteChange,
            format = { String.format(Locale.getDefault(), "%02d", it) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun WheelColumn(
    range: IntRange,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    format: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val items = remember(range) { range.toList() }
    val count = items.size
    val infiniteCount = count * 20
    val initialIndex = remember(items) {
        val base = (infiniteCount / 2) - ((infiniteCount / 2) % count)
        val itemOffset = items.indexOf(selectedValue).coerceAtLeast(0)
        (base + itemOffset - 1).coerceAtLeast(0)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) null
            else {
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
            }
        }
            .filterNotNull()
            .distinctUntilChangedBy { it.index }
            .collect { centerItem ->
                val actualValue = items[centerItem.index % count]
                if (actualValue != selectedValue) {
                    onValueChange(actualValue)
                }
            }
    }

    LaunchedEffect(selectedValue) {
        if (!listState.isScrollInProgress) {
            val currentCenterIndex = listState.firstVisibleItemIndex + 1
            val currentCenterValue = items[currentCenterIndex % count]
            if (currentCenterValue != selectedValue) {
                val base = (listState.firstVisibleItemIndex / count) * count
                val targetOffset = items.indexOf(selectedValue).coerceAtLeast(0)
                val targetIndex = (base + targetOffset - 1).coerceAtLeast(0)
                listState.scrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier.height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection Center Bar Highlight
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(46.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {}

        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 0.dp)
        ) {
            items(
                count = infiniteCount,
                key = { index -> index }
            ) { index ->
                val itemValue = items[index % count]
                val isSelected = itemValue == selectedValue

                val alpha = if (isSelected) 1f else 0.35f
                val scale = if (isSelected) 1.2f else 0.85f

                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(itemValue),
                        fontSize = 24.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .alpha(alpha)
                            .scale(scale)
                    )
                }
            }
        }
    }
}

// REPEAT SCHEDULE CARD COMPONENT (REDESIGNED & DECLUTTERED)
@Composable
fun RepeatScheduleCard(
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit
) {
    val isDaily = selectedDays.size == 7
    val isWeekdays = selectedDays == setOf(1, 2, 3, 4, 5)
    val isWeekends = selectedDays == setOf(6, 7)

    val summaryText = remember(selectedDays) {
        when {
            selectedDays.isEmpty() -> "Ring once only"
            selectedDays.size == 7 -> "Every day"
            selectedDays == setOf(1, 2, 3, 4, 5) -> "Monday to Friday"
            selectedDays == setOf(6, 7) -> "Saturday and Sunday"
            else -> "Custom days selected"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Repeat Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preset Segmented Selector Pill (3 Presets: Daily, Weekdays, Weekends)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(
                        "Daily" to setOf(1, 2, 3, 4, 5, 6, 7),
                        "Weekdays" to setOf(1, 2, 3, 4, 5),
                        "Weekends" to setOf(6, 7)
                    )

                    presets.forEach { (label, days) ->
                        val isPresetSelected = when (label) {
                            "Daily" -> isDaily
                            "Weekdays" -> isWeekdays
                            "Weekends" -> isWeekends
                            else -> false
                        }

                        val bg by animateColorAsState(
                            if (isPresetSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = "presetBg"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable {
                                    if (isPresetSelected) {
                                        onDaysChange(emptySet())
                                    } else {
                                        onDaysChange(days)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = if (isPresetSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clean 7-Day Pills (Equal Weight, Centered Text)
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayLabels.forEachIndexed { index, label ->
                    val dayNum = index + 1
                    val isSelected = selectedDays.contains(dayNum)

                    val bg by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        label = "dayBg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable {
                                val newDays = if (isSelected) {
                                    selectedDays - dayNum
                                } else {
                                    selectedDays + dayNum
                                }
                                onDaysChange(newDays)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// SNOOZE SETTINGS CARD COMPONENT (3-SEGMENT PILL: 5m, 10m, 15m)
@Composable
fun SnoozeSettingsCard(
    snoozeDurationMinutes: Int,
    onSnoozeDurationChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Snooze Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Max 3 times",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Select snooze interval",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Segment Pill Container (5 min, 10 min, 15 min)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val durations = listOf(5, 10, 15)
                    durations.forEach { duration ->
                        val isSelected = snoozeDurationMinutes == duration
                        val bg by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = "snoozeBg"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable { onSnoozeDurationChange(duration) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$duration min",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// BEDTIME REMINDER CARD COMPONENT
@Composable
fun BedtimeReminderCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bedtime Reminder 🌙",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Remind me 8 hours before alarm time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditAlarmScreenPreview() {
    SnoozeControlTheme {
        AddEditAlarmScreen(
            uiState = AddEditAlarmUiState(
                currentStep = 2,
                hour12 = 7,
                minute = 30,
                isAm = true,
                challengeType = ChallengeType.MATH,
                selectedDays = setOf(1, 2, 3, 4, 5)
            ),
            onHourChange = {},
            onMinuteChange = {},
            onAmPmChange = {},
            onChallengeTypeChange = {},
            onDaysChange = {},
            onSnoozeDurationChange = {},
            onNextStep = {},
            onPrevStep = {},
            onSave = {},
            onBack = {},
            onRegisterBarcodeClick = {}
        )
    }
}
