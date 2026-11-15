package com.hphc.library

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Two-finger pinch to resize reading text.
 *
 * Events are watched on the Initial pass and only consumed when a second
 * finger is down, so a single-finger drag still scrolls the page normally.
 * The callback receives the incremental scale factor of the gesture.
 */
@Composable
fun Modifier.pinchToZoom(onZoom: (Float) -> Unit): Modifier {
    val zoomCallback by rememberUpdatedState(onZoom)
    return this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.size >= 2) {
                    val zoom = event.calculateZoom()
                    if (zoom.isFinite() && zoom != 1f) {
                        zoomCallback(zoom)
                        event.changes.forEach { it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

/** Keeps a zoom factor inside the range the reader supports. */
fun clampZoom(zoom: Float): Float =
    zoom.coerceIn(LibraryPrefs.MIN_ZOOM, LibraryPrefs.MAX_ZOOM)
