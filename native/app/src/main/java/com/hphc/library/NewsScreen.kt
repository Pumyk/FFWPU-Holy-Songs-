package com.hphc.library

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

private fun prettyDate(iso: String): String = try {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    val date = parser.parse(iso) ?: return iso.take(10)
    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(date)
} catch (_: Exception) {
    iso.take(10)
}

/** 1 240 -> "1.2K", 3 400 000 -> "3.4M". */
private fun prettyViews(views: Long): String = when {
    views >= 1_000_000 -> String.format(Locale.US, "%.1fM views", views / 1_000_000f)
    views >= 1_000 -> String.format(Locale.US, "%.1fK views", views / 1_000f)
    views > 0 -> "$views views"
    else -> ""
}

/**
 * HJ Global News: each edition opens in the in-app player (YouTubeScreen)
 * instead of bouncing the viewer out to the YouTube app. Pull down to
 * refresh; editions you have already watched are marked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    items: List<NewsItem>,
    channel: String,
    refreshing: Boolean,
    watched: Set<String>,
    onRefresh: () -> Unit,
    onPlay: (NewsItem) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "HJ Global News",
                    fontFamily = Lora,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    if (refreshing) "Refreshing…" else "Tap an edition to watch · $channel",
                    fontFamily = Ui,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 2.dp, bottom = 12.dp)
                        .clickable { onRefresh() },
                )
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        "No editions loaded yet. Pull down, or tap the line above, to refresh.",
                        fontFamily = Ui,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(items, key = { it.id }) { n ->
                val latest = n == items.firstOrNull()
                val seen = watched.contains(n.id)
                Surface(
                    color = if (latest) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlay(n) },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    prettyDate(n.date).uppercase(Locale.getDefault()),
                                    fontFamily = Ui,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (seen) {
                                    Spacer(Modifier.width(7.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = CircleShape,
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(9.dp),
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                "WATCHED",
                                                fontFamily = Ui,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.6.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                n.title,
                                fontFamily = Lora,
                                fontSize = if (latest) 17.5.sp else 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = if (latest) 23.sp else 21.sp,
                            )
                            if (n.views > 0) {
                                Text(
                                    prettyViews(n.views),
                                    fontFamily = Ui,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    "Play in app",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    if (seen) "Again" else "Watch",
                                    fontFamily = Ui,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
