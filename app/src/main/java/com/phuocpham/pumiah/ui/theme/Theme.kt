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

@Composable
fun PumiahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
