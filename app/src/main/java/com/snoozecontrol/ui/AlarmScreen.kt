package com.snoozecontrol.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.model.AlarmItem
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    alarms: List<AlarmItem>,
    onAddAlarm: (hour: Int, minute: Int) -> Unit,
    onToggleAlarm: (id: Int) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snooze Control", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val calendar = Calendar.getInstance()
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        onAddAlarm(hourOfDay, minute)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
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
                Text(
                    text = "No alarms set",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmItemRow(
                        alarm = alarm,
                        onToggle = { onToggleAlarm(alarm.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmItemRow(
    alarm: AlarmItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = alarm.displayTime,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = if (alarm.isEnabled) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = if (alarm.isEnabled) "Alarm active" else "Alarm off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() }
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
        onAddAlarm = { _, _ -> },
        onToggleAlarm = {}
    )
}
