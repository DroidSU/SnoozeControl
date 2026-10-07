package com.snoozecontrol.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.ui.theme.SnoozeControlTheme
import com.snoozecontrol.util.Quote
import com.snoozecontrol.util.WeatherInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private object TtsManager {
    fun createTts(
        context: Context,
        onSpeakingChange: (Boolean) -> Unit
    ): TextToSpeech? {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingChange(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingChange(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakingChange(false)
                    }
                })
            }
        }
        return textToSpeech
    }

    fun speak(tts: Any?, text: String) {
        (tts as? TextToSpeech)?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "briefing_id")
    }

    fun stop(tts: Any?) {
        (tts as? TextToSpeech)?.stop()
    }

    fun shutdown(tts: Any?) {
        val t = tts as? TextToSpeech
        t?.stop()
        t?.shutdown()
    }
}

@Composable
fun MorningDashboardScreen(
    weatherInfo: WeatherInfo?,
    isWeatherLoading: Boolean,
    quote: Quote,
    userName: String = "Sujoy",
    onStartDayClick: () -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    // Real-time clock time
    var currentTimeText by remember {
        mutableStateOf(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()))
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            delay(1000L)
        }
    }

    val currentHour = remember(currentTimeText) { Calendar.getInstance()[Calendar.HOUR_OF_DAY] }
    val timeGreeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    val actualUserName = remember(userName, context) {
        if (userName.isNotBlank()) {
            userName
        } else {
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.getString("user_name", "Sujoy") ?: "Sujoy"
        }
    }

    val weatherText = remember(weatherInfo, isWeatherLoading) {
        if (weatherInfo != null) {
            val icon = when {
                weatherInfo.conditionText.contains("rain", ignoreCase = true) ||
                        weatherInfo.conditionText.contains("drizzle", ignoreCase = true) -> "🌧️"

                weatherInfo.conditionText.contains("snow", ignoreCase = true) -> "❄️"
                weatherInfo.conditionText.contains("cloud", ignoreCase = true) -> "⛅"
                weatherInfo.conditionText.contains("thunder", ignoreCase = true) -> "🌩️"
                weatherInfo.conditionText.contains("fog", ignoreCase = true) -> "🌫️"
                else -> "☀️"
            }
            "$icon ${weatherInfo.temperatureCelsius}°C · ${weatherInfo.conditionText}"
        } else if (isWeatherLoading) {
            "☀️ Checking weather..."
        } else {
            "☀️ 24°C · Clear"
        }
    }

    val currentDateText = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    // Text To Speech Morning Briefing state
    var tts by remember { mutableStateOf<Any?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var hasAutoPlayed by remember { mutableStateOf(false) }

    val startBriefing = remember(
        tts,
        weatherInfo,
        quote,
        timeGreeting,
        actualUserName,
        currentDateText,
        isInPreview
    ) {
        {
            if (!isInPreview && tts != null) {
                val weatherBriefing = if (weatherInfo != null)
                    "The current weather is ${weatherInfo.conditionText} with a temperature of ${weatherInfo.temperatureCelsius} degrees Celsius."
                else
                    ""
                val textToSpeak =
                    "$timeGreeting $actualUserName! Today is $currentDateText. $weatherBriefing Here is your daily quote: ${quote.text}. Have a wonderful day!"

                TtsManager.speak(tts, textToSpeak)
            }
        }
    }

    DisposableEffect(context, isInPreview) {
        if (isInPreview) {
            onDispose { }
        } else {
            val textToSpeech = TtsManager.createTts(context) { speaking ->
                isSpeaking = speaking
            }
            tts = textToSpeech

            onDispose {
                TtsManager.shutdown(textToSpeech)
            }
        }
    }

    LaunchedEffect(tts, weatherInfo, isWeatherLoading, isInPreview) {
        if (!isInPreview && tts != null && (!isWeatherLoading || weatherInfo != null) && !hasAutoPlayed) {
            hasAutoPlayed = true
            startBriefing()
        }
    }

    val toggleBriefing = {
        if (!isInPreview && tts != null) {
            if (isSpeaking) {
                TtsManager.stop(tts)
                isSpeaking = false
            } else {
                startBriefing()
            }
        }
    }

    val pulseScale = if (isSpeaking) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulseAudio")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        ).value
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Time, Greeting, Name, Weather, Quote
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // 1. Current Time
                Text(
                    text = currentTimeText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-1.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Time-of-day greeting
                Text(
                    text = timeGreeting,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 3. User name
                /**
                 * This section is commented out now, uncomment it when you add getting usernames.
                 */
//                Text(
//                    text = actualUserName,
//                    style = MaterialTheme.typography.headlineLarge.copy(
//                        fontSize = 32.sp,
//                        fontWeight = FontWeight.Bold
//                    ),
//                    color = MaterialTheme.colorScheme.onBackground
//                )
//
                Spacer(modifier = Modifier.height(20.dp))

                // 4. Weather / temperature
                Text(
                    text = weatherText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Short quote / motivational message
                Text(
                    text = "“${quote.text}”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Section: Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary Action: Voice Briefing Toggle
                OutlinedButton(
                    onClick = toggleBriefing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .scale(pulseScale),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                            alpha = 0.35f
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSpeaking) MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.4f
                        ) else MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isSpeaking) "Pause Voice Briefing 🔇" else "Play Voice Briefing 🎙️",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 6. Primary Action: Start My Day
                Button(
                    onClick = {
                        if (!isInPreview) {
                            TtsManager.stop(tts)
                        }
                        onStartDayClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        text = "Start My Day",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MorningDashboardScreenPreview() {
    SnoozeControlTheme {
        MorningDashboardScreen(
            weatherInfo = WeatherInfo(
                temperatureCelsius = 25,
                conditionText = "Sunny & Clear"
            ),
            isWeatherLoading = false,
            quote = Quote(
                text = "Start where you are. Use what you have. Do what you can.",
                author = "Arthur Ashe"
            ),
            userName = "Sujoy",
            onStartDayClick = {}
        )
    }
}
