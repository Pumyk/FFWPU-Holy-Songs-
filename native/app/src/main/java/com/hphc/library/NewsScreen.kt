package com.hphc.library

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private fun prettyDate(iso: String): String = try {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    val date = parser.parse(iso) ?: return iso.take(10)
    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(date)
} catch (_: Exception) {
    iso.take(10)
}

@Composable
fun NewsScreen(items: List<NewsItem>, channel: String, refreshing: Boolean, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("HJ Global News", fontFamily = Lora, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                if (refreshing) "Refreshing…" else "Latest English editions · $channel",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .clickable { onRefresh() },
            )
        }
        if (items.isEmpty()) {
            item { Text("No editions loaded yet. Tap to refresh.", fontSize = 14.sp) }
        }
        items(items, key = { it.id }) { n ->
            val latest = n == items.firstOrNull()
            Surface(
                color = if (latest) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Data.youtubeUrl(n.id))))
                    },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            prettyDate(n.date),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(n.title, fontSize = if (latest) 17.sp else 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
