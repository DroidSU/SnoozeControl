package com.snoozecontrol.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.ui.theme.SuccessGreen
import kotlin.math.sin

@Composable
fun LiquidProgressWave(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "liquidProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase1"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase2"
    )

    val bubbleOffset1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble1"
    )

    val bubbleOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble2"
    )

    val percentage = (animatedProgress * 100).toInt()

    Box(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    12.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = primaryColor.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.6f),
                            secondaryColor.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            val width = size.width
            val canvasHeight = size.height
            val cornerRadiusPx = 32.dp.toPx()

            val containerPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = androidx.compose.ui.geometry.Rect(0f, 0f, width, canvasHeight),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                )
            }

            clipPath(containerPath) {
                val liquidY = canvasHeight * (1f - animatedProgress)
                val waveAmplitude =
                    if (animatedProgress in 0.02f..0.98f) 12.dp.toPx() else 4.dp.toPx()
                val waveFrequency = 1.5f

                // Draw Back Wave
                val backWavePath = Path().apply {
                    moveTo(0f, canvasHeight)
                    lineTo(0f, liquidY)
                    var x = 0f
                    while (x <= width) {
                        val y =
                            liquidY + sin((x / width * 2 * Math.PI * waveFrequency) + wavePhase2).toFloat() * (waveAmplitude * 0.7f)
                        lineTo(x, y)
                        x += 8.dp.toPx()
                    }
                    lineTo(width, canvasHeight)
                    close()
                }

                drawPath(
                    path = backWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.7f),
                            secondaryColor.copy(alpha = 0.4f)
                        ),
                        startY = liquidY - waveAmplitude,
                        endY = canvasHeight
                    )
                )

                // Draw Front Wave
                val frontWavePath = Path().apply {
                    moveTo(0f, canvasHeight)
                    lineTo(0f, liquidY)
                    var x = 0f
                    while (x <= width) {
                        val y =
                            liquidY + sin((x / width * 2 * Math.PI * waveFrequency) + wavePhase1).toFloat() * waveAmplitude
                        lineTo(x, y)
                        x += 8.dp.toPx()
                    }
                    lineTo(width, canvasHeight)
                    close()
                }

                val activeFillColor = if (animatedProgress >= 0.99f) SuccessGreen else primaryColor

                drawPath(
                    path = frontWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeFillColor.copy(alpha = 0.95f),
                            secondaryColor.copy(alpha = 0.85f),
                            activeFillColor.copy(alpha = 0.6f)
                        ),
                        startY = liquidY - waveAmplitude,
                        endY = canvasHeight
                    )
                )

                // Floating Bubbles
                if (animatedProgress > 0.05f) {
                    val bubbleY1 = liquidY + (canvasHeight - liquidY) * bubbleOffset1
                    val bubbleY2 = liquidY + (canvasHeight - liquidY) * bubbleOffset2

                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = 6.dp.toPx(),
                        center = Offset(width * 0.3f, bubbleY1)
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = 4.dp.toPx(),
                        center = Offset(width * 0.7f, bubbleY2)
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = 8.dp.toPx(),
                        center = Offset(width * 0.5f, (bubbleY1 + bubbleY2) / 2)
                    )
                }
            }
        }

        // Percentage & Icon Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Shake Phone",
                        tint = if (percentage >= 100) SuccessGreen else primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 48.sp
            )

            Text(
                text = if (percentage >= 100) "TANK FILLED!" else "LIQUID TANK LEVEL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (percentage >= 100) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
    }
}
