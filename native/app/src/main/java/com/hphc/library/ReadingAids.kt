package com.hphc.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Plus Jakarta Sans - the alternative reading face offered for the pledge. */
val Jakarta = FontFamily(Font(R.font.plus_jakarta_sans))

/**
 * Holds the screen awake while a reading screen is open and the reader asked
 * for it. Uses the view flag, so nothing is left on once the screen closes.
 */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Pixel delta for one auto-scroll tick. Speed 0 is off; 10 is a brisk
 * hands-free crawl, roughly 140 px per second.
 */
fun autoScrollDelta(speed: Int): Float = speed.coerceIn(0, LibraryPrefs.MAX_SCROLL) * 0.45f

/** Small labelled chip used across the reading screens. */
@Composable
fun AidChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontFamily = Ui,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
