package com.hphc.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Holy Songs", fontFamily = Lora, fontSize = 31.sp, fontWeight = FontWeight.Bold)
            Text(
                "${songs.size} songs ready",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        SearchField(query) { query = it }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.id }) { song ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(song) },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (song.number != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    song.number,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                            Spacer(Modifier.size(12.dp))
                        }
                        Column {
                            Text(song.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (song.artist != null) {
                                Text(
                                    song.artist,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search title, number, or lyrics", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Renders lyrics: numbered verses, chorus shown once, generous type. */
@Composable
fun SongDetailScreen(song: Song, onBack: () -> Unit) {
    val blocks = remember(song.id) { lyricBlocks(song.lyrics) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 40.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.number != null) {
                        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)) {
                            Text(
                                song.number,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                        Spacer(Modifier.size(10.dp))
                    }
                    Text(song.title, fontFamily = Lora, fontSize = 29.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                }
                Spacer(Modifier.height(18.dp))
            }
            items(blocks) { block ->
                if (block.chorus) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("CHORUS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.4.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(block.text, fontFamily = Lora, fontSize = 18.sp, lineHeight = 30.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                } else {
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Text(
                            "${block.number}.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(width = 22.dp, height = 24.dp),
                        )
                        Text(block.text, fontFamily = Lora, fontSize = 18.sp, lineHeight = 30.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
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
    LazyColumn(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)) {
        item {
            Text("Family Pledge", fontFamily = Lora, fontSize = 31.sp, fontWeight = FontWeight.Bold)
            Text(
                "Eight pledges · centering on true love",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp),
            )
        }
        items(verses) { v ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(v.number, fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(v.text, fontFamily = Lora, fontSize = 17.sp, lineHeight = 28.sp)
                }
            }
        }
    }
}
