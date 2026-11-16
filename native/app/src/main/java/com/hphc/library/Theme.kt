package com.hphc.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ------------------------------------------------------------------ *
 * Type
 *
 * Lora (serif) carries everything you read: song titles, lyrics, the
 * pledge, book names. Inter (sans) carries the interface: section
 * labels, counts, dates, buttons. Two families, one rhythm.
 * ------------------------------------------------------------------ */

val Lora = FontFamily(Font(R.font.lora))

/** The face the app reads in by default. Serif, everywhere, unless a screen offers otherwise. */
val Ui = Lora

val Inter = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/* ------------------------------------------------------------------ *
 * Palette - warm paper / warm charcoal / true black
 * ------------------------------------------------------------------ */

val AmoledBlack = Color(0xFF000000)
val Cream = Color(0xFFF2E7D3)
val MutedWarm = Color(0xFF9C8E7A)
val Amber = Color(0xFFE9B44C)
val Ember = Color(0xFFC96A2B)

/** True-black scheme for AMOLED panels. */
private val AmoledScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    secondary = Ember,
    onSecondary = Color.Black,
    background = AmoledBlack,
    onBackground = Cream,
    surface = Color(0xFF0B0908),
    onSurface = Cream,
    surfaceVariant = Color(0xFF191512),
    onSurfaceVariant = MutedWarm,
    outline = Color(0xFF4A4038),
    outlineVariant = Color(0xFF2B2420),
)

/** Warm charcoal - easier than pure black in daylight. */
private val DarkScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    secondary = Ember,
    onSecondary = Color.Black,
    background = Color(0xFF14110E),
    onBackground = Cream,
    surface = Color(0xFF1B1714),
    onSurface = Cream,
    surfaceVariant = Color(0xFF272019),
    onSurfaceVariant = Color(0xFFB3A491),
    outline = Color(0xFF4C4237),
    outlineVariant = Color(0xFF322A23),
)

/** Warm paper. */
private val LightScheme = lightColorScheme(
    primary = Ember,
    onPrimary = Cream,
    secondary = Amber,
    onSecondary = Color(0xFF1F1A14),
    background = Color(0xFFFBF6EC),
    onBackground = Color(0xFF1F1A14),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF1F1A14),
    surfaceVariant = Color(0xFFF0E7D6),
    onSurfaceVariant = Color(0xFF6B5F4F),
    outline = Color(0xFFC9BAA2),
    outlineVariant = Color(0xFFE3D7C3),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 43.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Lora,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Lora,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Lora,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        letterSpacing = 1.4.sp,
    ),
)

/** How the app decides its colours. */
enum class ThemeMode(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
    Amoled("AMOLED");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: System
    }
}

@Composable
fun HphcTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val scheme = when (mode) {
        ThemeMode.System -> if (systemDark) DarkScheme else LightScheme
        ThemeMode.Light -> LightScheme
        ThemeMode.Dark -> DarkScheme
        ThemeMode.Amoled -> AmoledScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content,
    )
}

/** Small selector pill used by the appearance switcher. */
@Composable
fun ThemePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontFamily = Lora,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
