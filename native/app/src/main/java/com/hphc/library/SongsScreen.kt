package com.hphc.library

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SongsScreen(songs: List<Song>, onOpen: (Song) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, true) || it.number == query ||
                it.lyrics.contains(query, true) || (it.artist ?: "").contains(query, true)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Holy Songs", fontFamily = Lora, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "${songs.size} songs ready",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            SearchField(query) { query = it }
        }
        if (filtered.isEmpty()) {
            Text(
                "No song matches \"$query\"",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            items(filtered, key = { it.id }) { song ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(song) }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (song.number != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(7.dp),
                        ) {
                            Text(
                                song.number,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text(song.title, fontFamily = Lora, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        if (song.artist != null) {
                            Text(
                                song.artist,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(9.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        "Search title, number, or lyrics",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

/** Song page: compact header (top bar handles back), verse/chorus lyrics, A-/A+ zoom. */
@Composable
fun SongDetailScreen(song: Song) {
    val blocks = remember(song.id) { lyricBlocks(song.lyrics) }
    var zoom by rememberSaveable(song.id) { mutableFloatStateOf(1f) }
    val lyricSize = 18f * zoom
    val lyricLine = 30f * zoom

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 44.dp),
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.number != null) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)) {
                        Text(
                            song.number,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                }
                Text(
                    song.title,
                    fontFamily = Lora,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 31.sp,
                )
            }
            if (song.artist != null) {
                Text(
                    song.artist,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ZoomButton("A−") { if (zoom > 0.85f) zoom -= 0.1f }
                Text(
                    "${(zoom * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                ZoomButton("A+") { if (zoom < 1.6f) zoom += 0.1f }
            }
            Spacer(Modifier.height(14.dp))
        }
        items(blocks) { block ->
            if (block.chorus) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                ) {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
                        Text(
                            "CHORUS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(block.text, fontFamily = Lora, fontSize = lyricSize.sp, lineHeight = lyricLine.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Row(Modifier.padding(vertical = 5.dp)) {
                    Text(
                        "${block.number}.",
                        fontFamily = Lora,
                        fontSize = (lyricSize - 4).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(18.dp),
                    )
                    Text(block.text, fontFamily = Lora, fontSize = lyricSize.sp, lineHeight = lyricLine.sp)
                }
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

data class LyricBlock(val number: Int?, val text: String, val chorus: Boolean)

fun lyricBlocks(lyrics: String): List<LyricBlock> {
    val cleaned = lyrics.replace(Regex("(?i)chorus:\\s*"), "[CHORUS]\n")
    return cleaned.split(Regex("\n\\s*\n"))
        .filter { it.isNotBlank() }
        .mapIndexed { idx, raw ->
            val isChorus = raw.contains("[CHORUS]")
            LyricBlock(
                number = if (isChorus) null else idx + 1,
                text = raw.replace("[CHORUS]", "").trim(),
                chorus = isChorus,
            )
        }
}

@Composable
fun PledgeScreen(verses: List<PledgeVerse>) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
        item {
            Text("Family Pledge", fontFamily = Lora, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "Eight pledges · centering on true love",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )
        }
        items(verses, key = { it.number }) { v ->
            Row(Modifier.padding(bottom = 16.dp)) {
                Text(
                    v.number,
                    fontFamily = Lora,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(30.dp),
                )
                Text(
                    v.text,
                    fontFamily = Lora,
                    fontSize = 15.5.sp,
                    lineHeight = 25.sp,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}
