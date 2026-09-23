package com.snoozecontrol.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.R
import com.snoozecontrol.model.ChallengeType
import com.snoozecontrol.ui.theme.SnoozeControlTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    initialHour: Int = 7,
    initialMinute: Int = 0,
    initialChallenge: ChallengeType = ChallengeType.MATH,
    initialBarcode: String? = null,
    initialRepeatDays: String = "",
    resultBarcode: String? = null,
    onSave: (hour: Int, minute: Int, challengeType: ChallengeType, barcode: String?, repeatDays: String) -> Unit,
    onBack: () -> Unit,
    onRegisterBarcodeClick: () -> Unit
) {
    // 12-hour format conversion
    var isAm by remember { mutableStateOf(initialHour < 12) }
    var selectedHour12 by remember {
        mutableIntStateOf(
            when {
                initialHour == 0 -> 12
                initialHour > 12 -> initialHour - 12
                else -> initialHour
            }
        )
    }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    var selectedChallenge by remember { mutableStateOf(initialChallenge) }
    var scannedBarcode by remember { mutableStateOf(initialBarcode) }

    // Repeat schedule days: 1=Mon, 2=Tue, ..., 7=Sun
    var selectedDays by remember {
        mutableStateOf(
            if (initialRepeatDays.isBlank()) emptySet()
            else initialRepeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        )
    }

    LaunchedEffect(resultBarcode) {
        if (resultBarcode != null) {
            scannedBarcode = resultBarcode
        }
    }

    // Convert 12h + AM/PM back to 24h
    val final24Hour = remember(selectedHour12, isAm) {
        when {
            isAm && selectedHour12 == 12 -> 0
            isAm -> selectedHour12
            !isAm && selectedHour12 == 12 -> 12
            else -> selectedHour12 + 12
        }
    }

    val repeatDaysString = remember(selectedDays) {
        selectedDays.sorted().joinToString(",")
    }

    Scaffold(
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
                tonalElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        onSave(
                            final24Hour,
                            selectedMinute,
                            selectedChallenge,
                            scannedBarcode,
                            repeatDaysString
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = selectedChallenge != ChallengeType.BARCODE || scannedBarcode != null
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TIME SELECTION CARD
            TimePickerCard(
                hour12 = selectedHour12,
                minute = selectedMinute,
                isAm = isAm,
                onHourChange = { selectedHour12 = it },
                onMinuteChange = { selectedMinute = it },
                onAmPmChange = { isAm = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // REPEAT SCHEDULE SECTION
            RepeatScheduleCard(
                selectedDays = selectedDays,
                onDaysChange = { selectedDays = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // WAKE-UP CHALLENGE SECTION
            Text(
                text = stringResource(R.string.wakeup_challenge_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.wakeup_challenge_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(14.dp))

            ChallengeCard(
                title = stringResource(R.string.math_challenge_title),
                description = stringResource(R.string.math_challenge_desc),
                icon = Icons.Default.Calculate,
                isSelected = selectedChallenge == ChallengeType.MATH,
                showOpensScreenIndicator = false,
                onClick = { selectedChallenge = ChallengeType.MATH }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ChallengeCard(
                title = stringResource(R.string.barcode_challenge_title),
                description = stringResource(R.string.barcode_challenge_desc),
                icon = Icons.Default.QrCodeScanner,
                isSelected = selectedChallenge == ChallengeType.BARCODE,
                showOpensScreenIndicator = true,
                onClick = { selectedChallenge = ChallengeType.BARCODE }
            )

            if (selectedChallenge == ChallengeType.BARCODE) {
                Spacer(modifier = Modifier.height(14.dp))
                if (scannedBarcode == null) {
                    Card(
                        onClick = onRegisterBarcodeClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.scan_item_to_register),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Opens camera barcode scanner",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Barcode Registered",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(
                                        R.string.barcode_format,
                                        scannedBarcode ?: ""
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(onClick = onRegisterBarcodeClick) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.rescan_button),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = amPmText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Wheel Selector Body
            CustomDrumTimePicker(
                hour12 = hour12,
                minute = minute,
                onHourChange = onHourChange,
                onMinuteChange = onMinuteChange
            )

            Spacer(modifier = Modifier.height(16.dp))

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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(48.dp)
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(amBg)
                    .clickable { onAmPmChange(true) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(pmBg)
                    .clickable { onAmPmChange(false) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
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
            .height(170.dp),
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
            fontSize = 32.sp,
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
    val infiniteCount = count * 100
    val initialIndex = remember(items) {
        val base = (infiniteCount / 2) - ((infiniteCount / 2) % count)
        val itemOffset = items.indexOf(selectedValue).coerceAtLeast(0)
        (base + itemOffset - 1).coerceAtLeast(0)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisible ->
                val centerIndex = firstVisible + 1
                val actualValue = items[centerIndex % count]
                if (actualValue != selectedValue && listState.isScrollInProgress) {
                    onValueChange(actualValue)
                }
            }
    }

    Box(
        modifier = modifier.height(170.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection Center Bar Highlight
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
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
            items(infiniteCount) { index ->
                val itemValue = items[index % count]
                val isSelected = itemValue == selectedValue

                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.35f,
                    label = "wheelAlpha"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 0.85f,
                    label = "wheelScale"
                )

                Box(
                    modifier = Modifier
                        .height(56.dp)
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

// REPEAT SCHEDULE CARD COMPONENT
@Composable
fun RepeatScheduleCard(
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit
) {
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

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontally Scrollable Preset Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isOnce = selectedDays.isEmpty()
                val isDaily = selectedDays.size == 7
                val isWeekdays = selectedDays == setOf(1, 2, 3, 4, 5)
                val isWeekends = selectedDays == setOf(6, 7)

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

            Spacer(modifier = Modifier.height(18.dp))

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
                                if (isSelected) {
                                    onDaysChange(selectedDays - dayNum)
                                } else {
                                    onDaysChange(selectedDays + dayNum)
                                }
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
            onSave = { _, _, _, _, _ -> },
            onBack = {},
            onRegisterBarcodeClick = {}
        )
    }
}
