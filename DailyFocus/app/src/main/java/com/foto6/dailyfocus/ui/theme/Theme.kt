package com.foto6.dailyfocus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9F97D),
    onPrimary = Color(0xFF102107),
    secondary = Color(0xFF8FC7FF),
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFF4F6F8),
    surface = Color(0xFF14181E),
    onSurface = Color(0xFFF4F6F8),
    surfaceVariant = Color(0xFF20252D),
    onSurfaceVariant = Color(0xFFB9C0CA),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF2A0503)
)

@Composable
fun DailyFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
