package com.hphc.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/* ------------------------------------------------------------------ *
 * Song list
 * ------------------------------------------------------------------ */

@Composable
fun SongsScreen(
    songs: List<Song>,
    favorites: Set<String>,
    recentIds: List<String>,
    playlists: Map<String, Set<String>>,
    onToggleFavorite: (String) -> Unit,
    onOpen: (Song) -> Unit,
    onRemoveFromSet: (String, String) -> Unit,
    selectedId: String? = null,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(ALL) }
    var jumpOpen by remember { mutableStateOf(false) }

    val setName = filter.removePrefix(SET_PREFIX).takeIf { filter.startsWith(SET_PREFIX) }
    val setIds = setName?.let { playlists[it] }

    val searched = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, true) || (it.number ?: "").equals(query, true) ||
                (it.number ?: "").contains(query, true) ||
                it.lyrics.contains(query, true) || (it.artist ?: "").contains(query, true)
        }
    }
    val filtered = remember(searched, filter, setIds, favorites) {
        when {
            setIds != null -> searched.filter { setIds.contains(it.id) }
            filter == FAVORITES -> searched.filter { favorites.contains(it.id) }
            else -> searched
        }
    }
    val hymnCount = remember(songs) { songs.count { it.number != null } }
    val recents = remember(recentIds, songs) {
        recentIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
    }

    if (jumpOpen) {
        NumberPadDialog(
            songs = songs,
            onDismiss = { jumpOpen = false },
            onOpen = {
                jumpOpen = false
                onOpen(it)
            },
        )
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "Holy Songs",
                fontFamily = Lora,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                when {
                    setIds != null -> "${filtered.size} in $setName"
                    filter == FAVORITES -> "${favorites.size} saved"
                    else -> "$hymnCount hymns · offline"
                },
                fontFamily = Ui,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SearchField(query) { query = it } }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { jumpOpen = true },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "#",
                            fontFamily = Ui,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterPill("All $hymnCount", selected = filter == ALL) { filter = ALL }
                FilterPill("Favorites ${favorites.size}", selected = filter == FAVORITES) {
                    filter = FAVORITES
                }
                playlists.forEach { (name, ids) ->
                    FilterPill("$name ${ids.size}", selected = filter == SET_PREFIX + name) {
                        filter = SET_PREFIX + name
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (recents.isNotEmpty() && query.isBlank() && filter == ALL) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "RECENTLY OPENED",
                    fontFamily = Ui,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(recents, key = { "recent-${it.id}" }) { song ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.clickable { onOpen(song) },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (song.number != null) {
                                Text(
                                    "#${song.number}",
                                    fontFamily = Ui,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                song.title,
                                fontFamily = Ui,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            Text(
                when {
                    setIds != null && (setIds.isEmpty()) ->
                        "\"$setName\" is empty. Open a song and tap Add to set to build it."
                    setIds != null -> "Nothing in \"$setName\" matches \"$query\""
                    filter == FAVORITES && query.isBlank() ->
                        "No favorites yet. Tap the heart on any song to keep it here."
                    filter == FAVORITES -> "No favorite matches \"$query\""
                    else -> "No song matches \"$query\""
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(20.dp),
            )
        }
        val numbered = filtered.filter { it.number != null }
        val extras = filtered.filter { it.number == null }
        val showAnthemLabel = extras.isNotEmpty() && filter == ALL && query.isBlank()
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            items(numbered, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    starred = favorites.contains(song.id),
                    current = song.id == selectedId,
                    setName = setName,
                    onRemoveFromSet = onRemoveFromSet,
                    onToggleFavorite = onToggleFavorite,
                    onOpen = onOpen,
                )
            }
            if (showAnthemLabel) {
                item {
                    Text(
                        "ANTHEM",
                        fontFamily = Ui,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
            }
            items(extras, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    starred = favorites.contains(song.id),
                    current = song.id == selectedId,
                    setName = setName,
                    onRemoveFromSet = onRemoveFromSet,
                    onToggleFavorite = onToggleFavorite,
                    onOpen = onOpen,
                )
            }
        }
    }
}

/** One hymn row: number chip, title, set-remove and favourite. */
@Composable
private fun SongRow(
    song: Song,
    starred: Boolean,
    current: Boolean,
    setName: String?,
    onRemoveFromSet: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpen: (Song) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(song) }
            .background(
                if (current) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
            )
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
                    fontFamily = Ui,
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
                    fontFamily = Ui,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (setName != null) {
            IconButton(onClick = { onRemoveFromSet(setName, song.id) }) {
                Icon(
                    Icons.Default.Close,
                    "Remove from $setName",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(15.dp),
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
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp),
    )
}

private const val ALL = "all"
private const val FAVORITES = "favorites"
private const val SET_PREFIX = "set:"

/* ------------------------------------------------------------------ *
 * Quick jump: a compact number pad for service use
 * ------------------------------------------------------------------ */

@Composable
private fun NumberPadDialog(songs: List<Song>, onDismiss: () -> Unit, onOpen: (Song) -> Unit) {
    var input by remember { mutableStateOf("") }
    val numbered = remember(songs) { songs.filter { it.number != null } }
    val candidates = remember(input, numbered) {
        if (input.isBlank()) emptyList()
        else numbered.filter { it.number!!.startsWith(input, true) }.take(8)
    }
    val exact = remember(input, numbered) {
        numbered.firstOrNull { it.number.equals(input, true) }
    }

    fun press(key: String) {
        input = when (key) {
            "⌫" -> input.dropLast(1)
            "C" -> ""
            else -> (input + key).take(3)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = {
            Column {
                Text("Jump to a song", fontFamily = Lora, fontWeight = FontWeight.Bold)
                Text(
                    if (input.isBlank()) "Type a hymn number" else "#$input",
                    fontFamily = Ui,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            Column {
                if (exact != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(exact) },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "#${exact.number}",
                                fontFamily = Ui,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                exact.title,
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (exact == null && candidates.isNotEmpty()) {
                    candidates.forEach { song ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(song) }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "#${song.number}",
                                fontFamily = Ui,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(48.dp),
                            )
                            Text(
                                song.title,
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("a", "0", "b"),
                    listOf("⌫", "C"),
                )
                keys.forEach { rowKeys ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        rowKeys.forEach { key ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { press(key) },
                            ) {
                                Box(
                                    Modifier.padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        key,
                                        fontFamily = Ui,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        },
    )
}

/* ------------------------------------------------------------------ *
 * Song page
 * ------------------------------------------------------------------ */

/** Song page: lyrics, zoom, favorites, reading aids and prev/next navigation. */
@Composable
fun SongDetailScreen(
    song: Song,
    position: Int,
    total: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    zoom: Float,
    onZoom: (Float) -> Unit,
    hideChorus: Boolean,
    onToggleChorus: () -> Unit,
    keepAwake: Boolean,
    onToggleKeepAwake: () -> Unit,
    autoSpeed: Int,
    onAutoSpeed: (Int) -> Unit,
    playlists: Map<String, Set<String>>,
    onAddToSet: (String, String) -> Unit,
    onCreateSet: (String, String) -> Unit,
    onPrev: (() -> Unit)?,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val blocks = remember(song.id) { lyricBlocks(song.lyrics) }
    val visible = remember(blocks, hideChorus) {
        if (hideChorus) blocks.filter { !it.chorus } else blocks
    }
    val lyricSize = 18f * zoom
    val lyricLine = lyricSize * 1.7f
    val listState = rememberLazyListState()
    var setDialog by remember { mutableStateOf(false) }

    KeepScreenOn(keepAwake)
    LaunchedEffect(autoSpeed) {
        if (autoSpeed <= 0) return@LaunchedEffect
        while (true) {
            delay(32)
            listState.scrollBy(autoScrollDelta(autoSpeed))
        }
    }
    LaunchedEffect(song.id) { listState.scrollToItem(0) }

    if (setDialog) {
        AddToSetDialog(
            song = song,
            playlists = playlists,
            onAdd = { onAddToSet(it, song.id) },
            onCreate = { name -> onCreateSet(name, song.id) },
            onDismiss = { setDialog = false },
        )
    }

    // Plain-text version of the song for the share sheet / clipboard.
    val shareText = remember(song.id, blocks) {
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

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .pinchToZoom { factor -> onZoom(clampZoom(zoom * factor)) },
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
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
                        fontFamily = Ui,
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AidChip(
                        if (keepAwake) "Screen stays on" else "Keep screen on",
                        keepAwake,
                    ) { onToggleKeepAwake() }
                    AidChip(
                        if (autoSpeed == 0) "Auto-scroll off" else "Auto ×$autoSpeed",
                        autoSpeed > 0,
                    ) { onAutoSpeed(if (autoSpeed == 0) 4 else 0) }
                    if (autoSpeed > 0) {
                        AidChip("Slower", false) {
                            onAutoSpeed((autoSpeed - 1).coerceAtLeast(1))
                        }
                        AidChip("Faster", false) {
                            onAutoSpeed((autoSpeed + 1).coerceAtMost(LibraryPrefs.MAX_SCROLL))
                        }
                    }
                    AidChip(
                        if (hideChorus) "Chorus hidden" else "Chorus shown",
                        hideChorus,
                    ) { onToggleChorus() }
                    AidChip("Add to set", false) { setDialog = true }
                }
                Spacer(Modifier.height(14.dp))
            }
            items(visible) { block ->
                if (block.chorus) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                                Text(
                                    "CHORUS",
                                    fontFamily = Ui,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.6.sp,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    block.text,
                                    fontFamily = Lora,
                                    fontSize = lyricSize.sp,
                                    lineHeight = lyricLine.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape,
                            ) {
                                Text(
                                    "${block.number}",
                                    fontFamily = Ui,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }
                            Spacer(Modifier.width(11.dp))
                            Text(
                                block.text,
                                fontFamily = Lora,
                                fontSize = lyricSize.sp,
                                lineHeight = lyricLine.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (hideChorus && blocks.any { it.chorus }) {
                item {
                    Text(
                        "Chorus hidden · tap Chorus shown above to bring it back",
                        fontFamily = Ui,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        PrevNextBar(
            position = position,
            total = total,
            songNumber = song.number,
            hasPrev = onPrev != null,
            hasNext = onNext != null,
            onPrev = onPrev,
            onNext = onNext,
        )
    }
}

/** Bottom pager for stepping through the hymn book in order. */
@Composable
private fun PrevNextBar(
    position: Int,
    total: Int,
    songNumber: String?,
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: (() -> Unit)?,
    onNext: (() -> Unit)?,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onPrev?.invoke() }, enabled = hasPrev) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    "Previous song",
                    tint = if (hasPrev) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                )
            }
            Text(
                if (songNumber == null) "Anthem" else "${position + 1} of $total",
                fontFamily = Ui,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onNext?.invoke() }, enabled = hasNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    "Next song",
                    tint = if (hasNext) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                )
            }
        }
    }
}

/** Add the current song to a service set, or start a new one. */
@Composable
private fun AddToSetDialog(
    song: Song,
    playlists: Map<String, Set<String>>,
    onAdd: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = {
            Text(
                "Add \"${song.title}\" to a set",
                fontFamily = Lora,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
        },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(
                        "No sets yet. Create one below - Sunday Service, Morning Hoon Dok Hae, anything.",
                        fontFamily = Ui,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                playlists.forEach { (name, ids) ->
                    val inside = ids.contains(song.id)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { if (!inside) onAdd(name) }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            fontFamily = Ui,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (inside) "added" else "${ids.size} songs",
                            fontFamily = Ui,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (inside) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (newName.isEmpty()) {
                                Text(
                                    "New set name",
                                    fontFamily = Ui,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            BasicTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        IconButton(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    onCreate(newName.trim())
                                    newName = ""
                                }
                            },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                Icons.Default.Add,
                                "Create set",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
            }
        },
    )
}

/* ------------------------------------------------------------------ *
 * Shared bits
 * ------------------------------------------------------------------ */

@Composable
fun ZoomButton(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontFamily = Ui,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        )
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
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontSize = 11.5.sp,
            fontFamily = Ui,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

data class LyricBlock(val number: Int?, val text: String, val chorus: Boolean)

fun lyricBlocks(lyrics: String): List<LyricBlock> {
    val cleaned = lyrics.replace(Regex("(?i)chorus:\\s*"), "[CHORUS]\n")
    var verse = 0
    return cleaned.split(Regex("\n\\s*\n"))
        .filter { it.isNotBlank() }
        .map { raw ->
            val isChorus = raw.contains("[CHORUS]")
            val number = if (isChorus) null else ++verse
            LyricBlock(
                number = number,
                text = raw.replace("[CHORUS]", "").trim(),
                chorus = isChorus,
            )
        }
}
