package com.snoozecontrol.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.model.ChallengeType
import kotlin.random.Random

@Composable
fun AlarmDismissScreen(
    challengeType: ChallengeType,
    equation: String,
    answerInput: String,
    errorMessage: String?,
    onAnswerChange: (String) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onDismissClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (challengeType == ChallengeType.BARCODE) {
            BarcodeScannerView(
                onBarcodeDetected = onBarcodeScanned,
                modifier = Modifier.fillMaxSize()
            )
            
            // Overlay for Barcode Challenge
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        text = "Scan the registered Barcode to stop",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Wake Up!",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Solve to dismiss the alarm:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "$equation = ?",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = answerInput,
                    onValueChange = onAnswerChange,
                    label = { Text("Your Answer") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null,
                    singleLine = true,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismissClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Dismiss Alarm", fontSize = 18.sp)
                }
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun AlarmDismissScreenPreview() {
//    AlarmDismissScreen(
//        equation = "14 + 8",
//        answerInput = "",
//        errorMessage = null,
//        onAnswerChange = {},
//        onDismissClick = {}
//    )
//}

@Preview(showBackground = true)
@Composable
fun AlarmDismissScreenErrorPreview() {
    AlarmDismissScreen(
        challengeType = ChallengeType.MATH,
        equation = "12 * 4",
        answerInput = "42",
        errorMessage = "Incorrect answer, try again!",
        onAnswerChange = {},
        onBarcodeScanned = {},
        onDismissClick = {}
    )
}
