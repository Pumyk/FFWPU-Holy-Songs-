package com.hphc.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Family Pledge, in two shapes:
 *  - Read: the whole pledge in one scroll, for daily study.
 *  - Recite: one pledge per card, for focused recitation or memorising,
 *    with a "3 of 8" counter and swipe between pledges.
 */
@Composable
fun PledgeScreen(
    verses: List<PledgeVerse>,
    zoom: Float,
    onZoom: (Float) -> Unit,
    sans: Boolean,
    onSans: (Boolean) -> Unit,
    keepAwake: Boolean,
    onToggleKeepAwake: () -> Unit,
    autoSpeed: Int,
    onAutoSpeed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var recite by rememberSaveable { mutableStateOf(false) }
    val face = if (sans) Jakarta else Lora
    val body = 17f * zoom
    val leading = 29f * zoom

    KeepScreenOn(keepAwake)

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "Family Pledge",
                fontFamily = Lora,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ZoomButton("A−") {
                    if (zoom > LibraryPrefs.MIN_ZOOM + 0.001f) {
                        onZoom(clampZoom(zoom - LibraryPrefs.STEP_ZOOM))
                    }
                }
                Text(
                    "${(zoom * 100).toInt()}%",
                    fontFamily = Ui,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                ZoomButton("A+") {
                    if (zoom < LibraryPrefs.MAX_ZOOM - 0.001f) {
                        onZoom(clampZoom(zoom + LibraryPrefs.STEP_ZOOM))
                    }
                }
                Spacer(Modifier.width(12.dp))
                FilterPill("Read", selected = !recite) { recite = false }
                Spacer(Modifier.width(6.dp))
                FilterPill("Recite", selected = recite) { recite = true }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AidChip(if (sans) "Sans-serif" else "Serif", sans) { onSans(!sans) }
                AidChip(
                    if (keepAwake) "Screen stays on" else "Keep screen on",
                    keepAwake,
                ) { onToggleKeepAwake() }
                AidChip(
                    if (autoSpeed == 0) "Auto-scroll off" else "Auto ×$autoSpeed",
                    autoSpeed > 0,
                ) { onAutoSpeed(if (autoSpeed == 0) 4 else 0) }
                if (autoSpeed > 0) {
                    AidChip("Slower", false) { onAutoSpeed((autoSpeed - 1).coerceAtLeast(1)) }
                    AidChip("Faster", false) {
                        onAutoSpeed((autoSpeed + 1).coerceAtMost(LibraryPrefs.MAX_SCROLL))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (verses.isEmpty()) {
            Text(
                "The pledge text could not be loaded.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(20.dp),
            )
        } else if (recite) {
            RecitePager(verses, face, body, leading)
        } else {
            PledgeList(verses, face, body, leading, autoSpeed, zoom, onZoom)
        }
    }
}

@Composable
private fun PledgeList(
    verses: List<PledgeVerse>,
    face: FontFamily,
    body: Float,
    leading: Float,
    autoSpeed: Int,
    zoom: Float,
    onZoom: (Float) -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(autoSpeed) {
        if (autoSpeed <= 0) return@LaunchedEffect
        while (true) {
            delay(32)
            state.scrollBy(autoScrollDelta(autoSpeed))
        }
    }
    LazyColumn(
        state = state,
        modifier = Modifier.pinchToZoom { factor -> onZoom(clampZoom(zoom * factor)) },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
    ) {
        itemsIndexed(verses, key = { _, v -> v.number }) { idx, v ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                verses[idx].number,
                                fontFamily = Ui,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        v.text,
                        fontFamily = face,
                        fontSize = body.sp,
                        lineHeight = leading.sp,
                        letterSpacing = 0.1.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecitePager(
    verses: List<PledgeVerse>,
    face: FontFamily,
    body: Float,
    leading: Float,
) {
    val pager = rememberPagerState(pageCount = { verses.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            HorizontalPager(state = pager, contentPadding = PaddingValues(horizontal = 16.dp)) { page ->
                val verse = verses.getOrNull(page) ?: return@HorizontalPager
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 6.dp),
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                    ) {
                        Text(
                            "PLEDGE ${verse.number}",
                            fontFamily = Ui,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            verse.text,
                            fontFamily = face,
                            fontSize = (body + 2).sp,
                            lineHeight = (leading + 3).sp,
                            letterSpacing = 0.1.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        if (pager.currentPage > 0) pager.animateScrollToPage(pager.currentPage - 1)
                    }
                },
                enabled = pager.currentPage > 0,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    "Previous pledge",
                    tint = if (pager.currentPage > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                )
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${pager.currentPage + 1} of ${verses.size}",
                    fontFamily = Ui,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    verses.indices.forEach { i ->
                        Box(
                            Modifier
                                .size(if (i == pager.currentPage) 7.dp else 5.dp)
                                .background(
                                    if (i == pager.currentPage) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    scope.launch {
                        if (pager.currentPage < verses.size - 1) {
                            pager.animateScrollToPage(pager.currentPage + 1)
                        }
                    }
                },
                enabled = pager.currentPage < verses.size - 1,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    "Next pledge",
                    tint = if (pager.currentPage < verses.size - 1) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                )
            }
        }
        Text(
            "Swipe for the next pledge",
            fontFamily = Ui,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable { },
        )
    }
}
