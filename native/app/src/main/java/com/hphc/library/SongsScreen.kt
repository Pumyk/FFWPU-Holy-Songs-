package com.hphc.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SongsScreen(
    songs: List<Song>,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpen: (Song) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }

    val searched = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, true) || it.number == query ||
                it.lyrics.contains(query, true) || (it.artist ?: "").contains(query, true)
        }
    }
    val filtered = remember(searched, onlyFavorites, favorites) {
        if (onlyFavorites) searched.filter { favorites.contains(it.id) } else searched
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "Holy Songs",
                fontFamily = Lora,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                if (onlyFavorites) {
                    "${favorites.size} saved"
                } else {
                    "${songs.size} songs ready"
                },
                fontFamily = Inter,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            )
            Spacer(Modifier.height(12.dp))
            SearchField(query) { query = it }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("All ${songs.size}", selected = !onlyFavorites) { onlyFavorites = false }
                FilterPill("Favorites ${favorites.size}", selected = onlyFavorites) { onlyFavorites = true }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (filtered.isEmpty()) {
            Text(
                when {
                    onlyFavorites && query.isBlank() ->
                        "No favorites yet. Tap the heart on any song to keep it here."
                    onlyFavorites -> "No favorite matches \"$query\""
                    else -> "No song matches \"$query\""
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            items(filtered, key = { it.id }) { song ->
                val starred = favorites.contains(song.id)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(song) }
                        .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (song.number != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(7.dp),
                        ) {
                            Text(
                                song.number,
                                fontFamily = Inter,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            fontFamily = Lora,
                            fontSize = 17.5.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp,
                        )
                        if (song.artist != null) {
                            Text(
                                song.artist,
                                fontFamily = Inter,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                    }
                    IconButton(onClick = { onToggleFavorite(song.id) }) {
                        Icon(
                            if (starred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            if (starred) "Remove from favorites" else "Add to favorites",
                            tint = if (starred) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.size(18.dp),
                        )
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

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Song page: compact header (top bar handles back), verse/chorus lyrics, A-/A+ zoom, star + share. */
@Composable
fun SongDetailScreen(
    song: Song,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    zoom: Float,
    onZoom: (Float) -> Unit,
) {
    val ctx = LocalContext.current
    val blocks = remember(song.id) { lyricBlocks(song.lyrics) }
    val lyricSize = 18f * zoom
    val lyricLine = lyricSize * 1.7f

    // Plain-text version of the song for the share sheet / clipboard.
    val shareText = remember(song.id) {
        buildString {
            append(song.title)
            if (song.number != null) append(" (Holy Songs #").append(song.number).append(")")
            append("\n\n")
            append(
                blocks.joinToString("\n\n") { block ->
                    if (block.chorus) "Chorus:\n" + block.text else block.text
                }
            )
            append("\n\n— HPHC Library")
        }
    }

    fun shareSong() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, song.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(send, "Share lyrics")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(chooser) }
    }

    fun copySong() {
        val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clip.setPrimaryClip(ClipData.newPlainText(song.title, shareText))
        Toast.makeText(ctx, "Lyrics copied", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier.pinchToZoom { factor -> onZoom(clampZoom(zoom * factor)) },
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
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 33.sp,
                    letterSpacing = (-0.4).sp,
                )
            }
            if (song.artist != null) {
                Text(
                    song.artist,
                    fontFamily = Inter,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ZoomButton("A−") {
                    if (zoom > LibraryPrefs.MIN_ZOOM + 0.001f) {
                        onZoom(zoom - LibraryPrefs.STEP_ZOOM)
                    }
                }
                Text(
                    "${(zoom * 100).toInt()}%",
                    fontFamily = Inter,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                ZoomButton("A+") {
                    if (zoom < LibraryPrefs.MAX_ZOOM - 0.001f) {
                        onZoom(zoom + LibraryPrefs.STEP_ZOOM)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { copySong() }) {
                    Icon(
                        Icons.Default.ContentCopy, "Copy lyrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = { shareSong() }) {
                    Icon(
                        Icons.Default.Share, "Share lyrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
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
                            fontFamily = Inter,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.6.sp,
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
fun ZoomButton(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontFamily = Inter,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
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
fun PledgeScreen(verses: List<PledgeVerse>, zoom: Float, onZoom: (Float) -> Unit) {
    val body = 17f * zoom
    val leading = 29f * zoom

    LazyColumn(
        modifier = Modifier.pinchToZoom { factor -> onZoom(clampZoom(zoom * factor)) },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    ) {
        item {
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
                    fontFamily = Inter,
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
            }
            Spacer(Modifier.height(20.dp))
        }
        items(verses, key = { it.number }) { v ->
            Row(Modifier.padding(bottom = 18.dp)) {
                Text(
                    v.number,
                    fontFamily = Lora,
                    fontSize = (body - 2).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(26.dp),
                )
                Text(
                    v.text,
                    fontFamily = Lora,
                    fontSize = body.sp,
                    lineHeight = leading.sp,
                    letterSpacing = 0.1.sp,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
    }
}
