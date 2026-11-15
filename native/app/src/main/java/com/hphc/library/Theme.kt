package com.hphc.library

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// AMOLED black + mustard / orange / cream (Mfoniso's palette)
val Amoled = Color(0xFF000000)
val Night = Color(0xFF0A0A0A)
val Cream = Color(0xFFF2E7D3)
val MutedWarm = Color(0xFF9C8E7A)
val Amber = Color(0xFFE9B44C)
val Ember = Color(0xFFC96A2B)

private val Dark = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    secondary = Ember,
    background = Amoled,
    onBackground = Cream,
    surface = Night,
    onSurface = Cream,
    surfaceVariant = Color(0xFF171310),
    onSurfaceVariant = MutedWarm,
)

private val Light = lightColorScheme(
    primary = Ember,
    onPrimary = Cream,
    secondary = Amber,
    background = Color(0xFFFBF6EC),
    onBackground = Color(0xFF1F1A14),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF1F1A14),
    surfaceVariant = Color(0xFFF0E7D6),
    onSurfaceVariant = Color(0xFF6B5F4F),
)

@Composable
fun HphcTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = Typography(),
        content = content,
    )
}
