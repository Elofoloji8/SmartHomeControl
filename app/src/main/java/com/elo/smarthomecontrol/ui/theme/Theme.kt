package com.elo.smarthomecontrol.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    secondary = DarkSecondary,
    background = DarkPrimary,
    surface = DarkSecondary,
    onPrimary = DarkText,
    onSecondary = DarkText,
    onBackground = DarkText,
    onSurface = DarkText
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    secondary = LightSecondary,
    background = LightPrimary,
    surface = LightSecondary,
    onPrimary = LightText,
    onSecondary = LightText,
    onBackground = LightText,
    onSurface = LightText
)

@Composable
fun SmartHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}