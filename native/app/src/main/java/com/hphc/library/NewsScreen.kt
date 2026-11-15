package com.hphc.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

/**
 * HJ Global News: each edition opens in the in-app player (YouTubeScreen)
 * instead of bouncing the viewer out to the YouTube app.
 */
@Composable
fun NewsScreen(
    items: List<NewsItem>,
    channel: String,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onPlay: (NewsItem) -> Unit,
) {
    LazyColumn(
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
                fontFamily = Inter,
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
                    "No editions loaded yet. Tap the line above to refresh.",
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(items, key = { it.id }) { n ->
            val latest = n == items.firstOrNull()
            Surface(
                color = if (latest) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(n) },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            prettyDate(n.date).uppercase(Locale.getDefault()),
                            fontFamily = Inter,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            n.title,
                            fontFamily = Lora,
                            fontSize = if (latest) 17.5.sp else 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = if (latest) 23.sp else 21.sp,
                        )
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
                                "Watch",
                                fontFamily = Inter,
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
