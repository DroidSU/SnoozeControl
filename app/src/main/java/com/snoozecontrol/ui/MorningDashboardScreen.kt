package com.snoozecontrol.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snoozecontrol.ui.theme.SnoozeControlTheme
import com.snoozecontrol.util.Quote
import com.snoozecontrol.util.WeatherInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TtsManager {
    private var textToSpeech: TextToSpeech? = null
    private var isReady = false
    private var onSpeakingListener: ((Boolean) -> Unit)? = null
    private var onCompleteListener: (() -> Unit)? = null
    private var isManualStop = false

    fun prewarm(context: Context) {
        if (textToSpeech != null) return
        val appContext = context.applicationContext
        textToSpeech = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                textToSpeech?.language = Locale.US
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingListener?.invoke(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingListener?.invoke(false)
                        if (!isManualStop) {
                            onCompleteListener?.invoke()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakingListener?.invoke(false)
                    }
                })
            }
        }
    }

    fun setSpeakingListener(listener: ((Boolean) -> Unit)?) {
        onSpeakingListener = listener
    }

    fun setCompleteListener(listener: (() -> Unit)?) {
        onCompleteListener = listener
    }

    fun isReady(): Boolean = isReady && textToSpeech != null

    fun speak(text: String) {
        if (textToSpeech != null) {
            try {
                isManualStop = false
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "briefing_id")
            } catch (e: Exception) {
                Log.e("TtsManager", "Error speaking text", e)
            }
        }
    }

    fun stop() {
        isManualStop = true
        val tts = textToSpeech
        CoroutineScope(Dispatchers.IO).launch {
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.e("TtsManager", "Error stopping TTS", e)
            }
        }
    }

    fun shutdown() {
        onSpeakingListener = null
        onCompleteListener = null
        val ttsToShutdown = textToSpeech
        textToSpeech = null
        isReady = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ttsToShutdown?.stop()
                ttsToShutdown?.shutdown()
            } catch (e: Exception) {
                Log.e("TtsManager", "Error shutting down TTS", e)
            }
        }
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
    var isSpeaking by remember { mutableStateOf(false) }
    var hasAutoPlayed by remember { mutableStateOf(false) }

    val startBriefing = remember(
        weatherInfo,
        quote,
        timeGreeting,
        actualUserName,
        currentDateText,
        isInPreview
    ) {
        {
            if (!isInPreview) {
                val weatherBriefing = if (weatherInfo != null)
                    "The current weather is ${weatherInfo.conditionText} with a temperature of ${weatherInfo.temperatureCelsius} degrees Celsius."
                else
                    ""
                val textToSpeak =
                    "$timeGreeting $actualUserName! Today is $currentDateText. $weatherBriefing Here is your daily quote: ${quote.text}. Have a wonderful day!"

                TtsManager.speak(textToSpeak)
            }
        }
    }

    DisposableEffect(context, isInPreview, onStartDayClick) {
        if (isInPreview) {
            onDispose { }
        } else {
            TtsManager.prewarm(context)
            TtsManager.setSpeakingListener { speaking ->
                isSpeaking = speaking
            }
            TtsManager.setCompleteListener {
                onStartDayClick()
            }

            onDispose {
                TtsManager.setSpeakingListener(null)
                TtsManager.setCompleteListener(null)
            }
        }
    }

    LaunchedEffect(weatherInfo, isWeatherLoading, isInPreview) {
        if (!isInPreview && !hasAutoPlayed) {
            hasAutoPlayed = true
            startBriefing()
        }
    }

    val toggleBriefing = {
        if (!isInPreview) {
            if (isSpeaking) {
                TtsManager.stop()
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
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        ).value
    } else {
        1f
    }

    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.60f else 0.65f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.20f else 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Hero Cards
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Clock Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            8.dp,
                            RoundedCornerShape(28.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Greeting Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (currentHour in 6..18) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$timeGreeting, $actualUserName",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clock Time
                        Text(
                            text = currentTimeText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 58.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Full Date
                        Text(
                            text = currentDateText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 2. Weather Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = weatherText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Weather",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 3. Inspirational Quote Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DAILY INSPIRATION",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "“${quote.text}”",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!quote.author.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "— ${quote.author}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Section: Start My Day Primary Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (!isInPreview) {
                            TtsManager.stop()
                        }
                        onStartDayClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(
                            12.dp,
                            RoundedCornerShape(20.dp),
                            ambientColor = MaterialTheme.colorScheme.primary
                        ),
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Top-Right Speaker Icon Button
        IconButton(
            onClick = toggleBriefing,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 20.dp)
                .scale(pulseScale)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSpeaking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.35f
                    )
                )
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (isSpeaking) "Turn off voice briefing" else "Play voice briefing",
                        tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
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
