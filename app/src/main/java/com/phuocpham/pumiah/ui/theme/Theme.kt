package com.phuocpham.pumiah.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF6C5CE7)
val Green = Color(0xFF00B894)
val Yellow = Color(0xFFFDCB6E)
val Red = Color(0xFFE17055)
val Blue = Color(0xFF74B9FF)
val LightPurple = Color(0xFFA29BFE)
val Teal = Color(0xFF00CEC9)
val Pink = Color(0xFFFD79A8)

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = LightPurple,
    secondary = Green,
    onSecondary = Color.White,
    tertiary = Blue,
    error = Red,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onBackground = Color(0xFF2D3436),
    onSurface = Color(0xFF2D3436),
)

private val DarkColors = darkColorScheme(
    primary = LightPurple,
    onPrimary = Color(0xFF1A1A2E),
    primaryContainer = Purple,
    onPrimaryContainer = Color.White,
    secondary = Teal,
    onSecondary = Color(0xFF0A1F1F),
    tertiary = Blue,
    error = Red,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E2E),
    surfaceVariant = Color(0xFF2A2A3A),
    onBackground = Color(0xFFE8E8EA),
    onSurface = Color(0xFFE8E8EA),
    onSurfaceVariant = Color(0xFFB8B8C0),
)

@Composable
fun PumiahTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
