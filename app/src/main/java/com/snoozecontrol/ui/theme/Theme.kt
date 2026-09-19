package com.snoozecontrol.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SunsetCoral,
    secondary = SoftLavender,
    tertiary = MorningGold,
    background = MidnightAbyss,
    surface = SteelNavy,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = CloudWhite,
    onSurface = CloudWhite,
    error = ErrorRed,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = SunsetCoral,
    secondary = ElectricIndigo,
    tertiary = SoftPeach,
    background = CloudWhite,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = MidnightAbyss,
    onSurface = MidnightAbyss,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun SnoozeControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false to prioritize our custom palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dynamic color logic removed to simplify and enforce palette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
