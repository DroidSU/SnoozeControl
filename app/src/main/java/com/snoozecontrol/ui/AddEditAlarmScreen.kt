package com.snoozecontrol.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.R
import com.snoozecontrol.model.ChallengeType
import com.snoozecontrol.ui.theme.SnoozeControlTheme
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
    onDaysChange: (Set<Int>) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onRegisterBarcodeClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.set_alarm_title),
                        fontWeight = FontWeight.Bold
                    )
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
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = uiState.isSaveEnabled
                ) {
                    Text(
                        stringResource(R.string.save_alarm_button),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TIME SELECTION CARD
            TimePickerCard(
                hour12 = uiState.hour12,
                minute = uiState.minute,
                isAm = uiState.isAm,
                onHourChange = onHourChange,
                onMinuteChange = onMinuteChange,
                onAmPmChange = onAmPmChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // HERO WAKE-UP MISSION / CHALLENGE SECTION
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "MISSION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.wakeup_challenge_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.wakeup_challenge_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ChallengeCard(
                        title = stringResource(R.string.math_challenge_title),
                        description = stringResource(R.string.math_challenge_desc),
                        icon = Icons.Default.Calculate,
                        isSelected = uiState.challengeType == ChallengeType.MATH,
                        showOpensScreenIndicator = false,
                        onClick = { onChallengeTypeChange(ChallengeType.MATH) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ChallengeCard(
                        title = stringResource(R.string.barcode_challenge_title),
                        description = if (uiState.targetBarcode != null)
                            "Scan registered barcode to dismiss alarm."
                        else
                            stringResource(R.string.barcode_challenge_desc),
                        icon = Icons.Default.QrCodeScanner,
                        isSelected = uiState.challengeType == ChallengeType.BARCODE,
                        showOpensScreenIndicator = true,
                        registeredBarcode = uiState.targetBarcode,
                        onRescanClick = onRegisterBarcodeClick,
                        onClick = {
                            onChallengeTypeChange(ChallengeType.BARCODE)
                            if (uiState.targetBarcode == null) {
                                onRegisterBarcodeClick()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // REPEAT SCHEDULE SECTION
            RepeatScheduleCard(
                selectedDays = uiState.selectedDays,
                onDaysChange = onDaysChange
            )

            Spacer(modifier = Modifier.height(24.dp))
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Main Time Display
            val formattedTime = remember(hour12, minute) {
                String.format(Locale.getDefault(), "%02d:%02d", hour12, minute)
            }
            val amPmText = if (isAm) "AM" else "PM"
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPmText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Compact Wheel Selector Body
            CustomDrumTimePicker(
                hour12 = hour12,
                minute = minute,
                onHourChange = onHourChange,
                onMinuteChange = onMinuteChange
            )

            Spacer(modifier = Modifier.height(10.dp))

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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
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
                    .clip(RoundedCornerShape(13.dp))
                    .background(amBg)
                    .clickable { onAmPmChange(true) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(pmBg)
                    .clickable { onAmPmChange(false) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
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
            .height(135.dp),
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
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp)
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
        modifier = modifier.height(135.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection Center Bar Highlight
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
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
                val scale = if (isSelected) 1.18f else 0.85f

                Box(
                    modifier = Modifier
                        .height(45.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(itemValue),
                        fontSize = 22.sp,
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

// REPEAT SCHEDULE CARD COMPONENT
@Composable
fun RepeatScheduleCard(
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit
) {
    val isOnce = selectedDays.isEmpty()
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Repeat Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontally Scrollable Preset Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isOnce,
                    onClick = { onDaysChange(emptySet()) },
                    label = { Text("Once", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = isDaily,
                    onClick = { onDaysChange(setOf(1, 2, 3, 4, 5, 6, 7)) },
                    label = { Text("Daily", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = isWeekdays,
                    onClick = { onDaysChange(setOf(1, 2, 3, 4, 5)) },
                    label = {
                        Text(
                            "Weekdays",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = isWeekends,
                    onClick = { onDaysChange(setOf(6, 7)) },
                    label = {
                        Text(
                            "Weekends",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Days of Week Pills: Mon=1, Tue=2, Wed=3, Thu=4, Fri=5, Sat=6, Sun=7
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEachIndexed { index, label ->
                    val dayNum = index + 1
                    val isSelected = selectedDays.contains(dayNum)

                    val bg by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        label = "dayBg"
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
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
                            fontSize = 14.sp,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditAlarmScreenPreview() {
    SnoozeControlTheme {
        AddEditAlarmScreen(
            uiState = AddEditAlarmUiState(
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
            onSave = {},
            onBack = {},
            onRegisterBarcodeClick = {}
        )
    }
}
