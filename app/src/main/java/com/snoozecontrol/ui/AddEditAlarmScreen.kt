package com.snoozecontrol.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.R
import com.snoozecontrol.model.ChallengeType
import com.snoozecontrol.ui.theme.SnoozeControlTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    initialHour: Int = 7,
    initialMinute: Int = 0,
    initialChallenge: ChallengeType = ChallengeType.MATH,
    initialBarcode: String? = null,
    resultBarcode: String? = null,
    onSave: (hour: Int, minute: Int, challengeType: ChallengeType, barcode: String?) -> Unit,
    onBack: () -> Unit,
    onRegisterBarcodeClick: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    var selectedChallenge by remember { mutableStateOf(initialChallenge) }
    var scannedBarcode by remember { mutableStateOf(initialBarcode) }

    // Sync barcode result from dedicated registration screen
    LaunchedEffect(resultBarcode) {
        if (resultBarcode != null) {
            scannedBarcode = resultBarcode
        }
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
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_desc)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    onSave(
                        timePickerState.hour,
                        timePickerState.minute,
                        selectedChallenge,
                        scannedBarcode
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = selectedChallenge != ChallengeType.BARCODE || scannedBarcode != null
            ) {
                Text(
                    stringResource(R.string.save_alarm_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time Picker
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp)) {
                    TimeInput(state = timePickerState)
                }
            }

            Text(
                text = stringResource(R.string.wakeup_challenge_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = stringResource(R.string.wakeup_challenge_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(24.dp))

            ChallengeCard(
                title = stringResource(R.string.math_challenge_title),
                description = stringResource(R.string.math_challenge_desc),
                icon = Icons.Default.Calculate,
                isSelected = selectedChallenge == ChallengeType.MATH,
                onClick = { selectedChallenge = ChallengeType.MATH }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ChallengeCard(
                title = stringResource(R.string.barcode_challenge_title),
                description = stringResource(R.string.barcode_challenge_desc),
                icon = Icons.Default.QrCodeScanner,
                isSelected = selectedChallenge == ChallengeType.BARCODE,
                onClick = { selectedChallenge = ChallengeType.BARCODE }
            )

            if (selectedChallenge == ChallengeType.BARCODE) {
                Spacer(modifier = Modifier.height(24.dp))
                if (scannedBarcode == null) {
                    OutlinedButton(
                        onClick = onRegisterBarcodeClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.scan_item_to_register))
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.barcode_format, scannedBarcode ?: ""),
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onRegisterBarcodeClick) {
                                Text(stringResource(R.string.rescan_button))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom bar
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditAlarmScreenPreview() {
    SnoozeControlTheme {
        AddEditAlarmScreen(
            onSave = { _, _, _, _ -> },
            onBack = {},
            onRegisterBarcodeClick = {}
        )
    }
}

@Composable
fun ChallengeCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
