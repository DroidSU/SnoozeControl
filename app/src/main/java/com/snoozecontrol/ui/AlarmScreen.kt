package com.snoozecontrol.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.R
import com.snoozecontrol.model.AlarmItem
import com.snoozecontrol.model.ChallengeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    alarms: List<AlarmItem>,
    nextAlarm: AlarmItem?,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    onToggleAlarm: (Int) -> Unit,
    onDeleteAlarm: (AlarmItem) -> Unit
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (nextAlarm != null) stringResource(
                                R.string.next_alarm_format,
                                nextAlarm.displayTime
                            ) else stringResource(R.string.good_morning),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_alarm_desc),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.no_alarms_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.no_alarms_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    bottom = 88.dp,
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    SwipeToDismissRow(
                        alarm = alarm,
                        onDismiss = { onDeleteAlarm(alarm) },
                        onContentClick = { onEditClick(alarm.id) },
                        onToggle = { onToggleAlarm(alarm.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissRow(
    alarm: AlarmItem,
    onDismiss: () -> Unit,
    onContentClick: () -> Unit,
    onToggle: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                }, label = "dismissColor"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_alarm_desc),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false,
        content = {
            AlarmItemRow(
                alarm = alarm,
                onToggle = onToggle,
                onClick = onContentClick
            )
        }
    )
}

@Composable
fun AlarmItemRow(
    alarm: AlarmItem,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (alarm.isEnabled)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "containerColor"
    )

    val elevation by animateDpAsState(
        targetValue = if (alarm.isEnabled) 8.dp else 0.dp,
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val challengeIcon = when (alarm.challengeType) {
                        ChallengeType.MATH -> Icons.Default.Calculate
                        ChallengeType.BARCODE -> Icons.Default.QrCodeScanner
                        else -> Icons.Default.NotificationsActive
                    }
                    Icon(
                        imageVector = challengeIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alarm.challengeType.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = alarm.displayTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 44.sp
                    ),
                    color = if (alarm.isEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlarmScreenPreview() {
    val sampleAlarms = listOf(
        AlarmItem(1, 7, 0, true),
        AlarmItem(2, 8, 30, false),
        AlarmItem(3, 22, 15, true)
    )
    AlarmScreen(
        alarms = sampleAlarms,
        nextAlarm = sampleAlarms.first(),
        onAddClick = {},
        onEditClick = {},
        onToggleAlarm = {},
        onDeleteAlarm = {}
    )
}
