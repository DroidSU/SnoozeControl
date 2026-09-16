package com.snoozecontrol.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.snoozecontrol.model.ChallengeType

@Composable
fun AddAlarmChallengeDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (ChallengeType, String?) -> Unit
) {
    var selectedChallenge by remember { mutableStateOf(ChallengeType.MATH) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Configure Challenge",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = String.format("For %02d:%02d", hour, minute),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Select Challenge Type:", style = MaterialTheme.typography.labelLarge)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedChallenge == ChallengeType.MATH,
                        onClick = { selectedChallenge = ChallengeType.MATH }
                    )
                    Text("Math Problems")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedChallenge == ChallengeType.BARCODE,
                        onClick = { selectedChallenge = ChallengeType.BARCODE }
                    )
                    Text("Barcode Scanner")
                }

                if (selectedChallenge == ChallengeType.BARCODE) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (scannedBarcode == null) {
                        if (isScanning) {
                            Box(modifier = Modifier.size(200.dp)) {
                                BarcodeScannerView(onBarcodeDetected = {
                                    scannedBarcode = it
                                    isScanning = false
                                })
                            }
                        } else {
                            Button(onClick = { isScanning = true }) {
                                Text("Scan Item to Register")
                            }
                        }
                    } else {
                        Text("Barcode Registered: $scannedBarcode", color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { scannedBarcode = null }) {
                            Text("Rescan")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedChallenge, scannedBarcode) },
                        enabled = selectedChallenge != ChallengeType.BARCODE || scannedBarcode != null
                    ) {
                        Text("Set Alarm")
                    }
                }
            }
        }
    }
}
