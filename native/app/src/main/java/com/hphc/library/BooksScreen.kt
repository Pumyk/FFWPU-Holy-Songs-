package com.hphc.library

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/* ------------------------------------------------------------------ *
 * Shelf
 * ------------------------------------------------------------------ */

@Composable
fun BooksScreen(
    remoteBooks: List<Book>,
    refreshing: Boolean,
    prefs: LibraryPrefs,
    onOpen: (Book) -> Unit,
) {
    val shelf = Data.bundledBooks + remoteBooks
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            Text("Books", fontFamily = Lora, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Full texts, offline",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (refreshing) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(shelf, key = { it.id }) { book ->
            val total = if (book.structured) 0 else prefs.pdfTotal(book.id)
            val page = prefs.pdfPage(book.id)
            val started = total > 0 && page > 0
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(book) },
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    when {
                                        book.structured -> Icons.Default.Description
                                        book.remote -> Icons.Default.Cloud
                                        else -> Icons.Default.Description
                                    },
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(book.title, fontFamily = Lora, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (book.author.isNotBlank()) {
                                Text(
                                    book.author,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                when {
                                    book.structured -> book.note
                                    started -> "Page $page of $total"
                                    book.remote -> "From the cloud · tap to open"
                                    else -> book.note
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (started && total > 0) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (page.toFloat() / total).coerceIn(0f, 1f) },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ *
 * PDF reading filter
 * ------------------------------------------------------------------ */

private val NightMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

private val WarmMatrix = ColorMatrix(
    floatArrayOf(
        0.93f, 0f, 0f, 0f, 5f,
        0f, 0.85f, 0f, 0f, 3f,
        0f, 0f, 0.70f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

@Composable
private fun pdfFilter(mode: Int): ColorFilter? = when (mode) {
    1 -> ColorFilter.colorMatrix(NightMatrix)
    2 -> ColorFilter.colorMatrix(WarmMatrix)
    else -> null
}

private fun filterLabel(mode: Int): String = when (mode) {
    1 -> "Night"
    2 -> "Warm"
    else -> "Filter off"
}

/* ------------------------------------------------------------------ *
 * PDF reader
 * ------------------------------------------------------------------ */

/**
 * Native PDF reader: lazy page rendering, per-book reading position,
 * named bookmarks, page jump, a night/warm reading filter and a
 * pinch-to-zoom full-page view.
 */
@Composable
fun PdfBookScreen(book: Book, prefs: LibraryPrefs) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var file by remember { mutableStateOf<File?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    val pageRatio = remember { mutableStateMapOf<Int, Float>() }
    val bitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = prefs.pdfPage(book.id),
    )

    val pageTexts = remember(book.id) { Data.pageText(ctx, book.id) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    var filter by remember { mutableIntStateOf(prefs.pdfFilter) }
    var jumpOpen by remember { mutableStateOf(false) }
    var marksOpen by remember { mutableStateOf(false) }
    var marks by remember { mutableStateOf(prefs.bookmarks(book.id)) }
    var zoomPage by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(book.id) {
        val f = withContext(Dispatchers.IO) { Data.pdfFile(ctx, book) }
        file = f
        failed = f == null
        loading = false
    }

    DisposableEffect(file) {
        val f = file
        if (f == null) return@DisposableEffect onDispose { }
        val descriptor = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        val r = PdfRenderer(descriptor)
        pfd = descriptor
        renderer = r
        pageCount = r.pageCount
        prefs.setPdfTotal(book.id, r.pageCount)
        for (i in 0 until r.pageCount) {
            r.openPage(i).use { p -> pageRatio[i] = p.width.toFloat() / p.height.toFloat() }
        }
        onDispose {
            bitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
            renderer = null
            pfd = null
        }
    }

    LaunchedEffect(book.id) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { idx ->
            prefs.setPdfPage(book.id, idx)
        }
    }

    val results = remember(query, pageTexts) {
        if (query.length < 2 || pageTexts == null) emptyList()
        else pageTexts.withIndex()
            .filter { it.value.contains(query, true) }
            .take(40)
            .map { it.index to it.value }
    }

    if (jumpOpen && pageCount > 0) {
        PageJumpDialog(
            pageCount = pageCount,
            current = state.firstVisibleItemIndex + 1,
            onGo = { page ->
                jumpOpen = false
                scope.launch { state.scrollToItem((page - 1).coerceIn(0, pageCount - 1)) }
            },
            onDismiss = { jumpOpen = false },
        )
    }

    if (marksOpen) {
        BookmarksDialog(
            marks = marks,
            currentPage = state.firstVisibleItemIndex + 1,
            onAdd = { label ->
                val next = (marks + Bookmark(state.firstVisibleItemIndex + 1, label))
                    .distinctBy { it.page }
                marks = next
                prefs.saveBookmarks(book.id, next)
            },
            onDelete = { mark ->
                val next = marks - mark
                marks = next
                prefs.saveBookmarks(book.id, next)
            },
            onGo = { page ->
                marksOpen = false
                scope.launch { state.scrollToItem((page - 1).coerceIn(0, pageCount - 1)) }
            },
            onDismiss = { marksOpen = false },
        )
    }

    val zoomIndex = zoomPage
    if (zoomIndex != null) {
        val bmp = bitmaps[zoomIndex]
        if (bmp != null) {
            ZoomPageDialog(
                bitmap = bmp,
                colorFilter = pdfFilter(filter),
                page = zoomIndex + 1,
                total = pageCount,
                onClose = { zoomPage = null },
            )
        } else {
            LaunchedEffect(zoomIndex) { zoomPage = null }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
            ) {
                Text(book.title, fontFamily = Lora, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    when {
                        loading -> "preparing…"
                        failed -> "couldn't load this book"
                        else -> "page ${state.firstVisibleItemIndex + 1} / $pageCount"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AidChip(filterLabel(filter), filter != 0) {
                filter = (filter + 1) % 3
                prefs.pdfFilter = filter
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = { jumpOpen = true }, modifier = Modifier.size(34.dp)) {
                Text(
                    "#",
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { marksOpen = true }, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (marks.any { it.page == state.firstVisibleItemIndex + 1 }) {
                        Icons.Default.Bookmark
                    } else {
                        Icons.Default.BookmarkBorder
                    },
                    "Bookmarks",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp),
                )
            }
            if (pageTexts != null) {
                IconButton(onClick = { searchOpen = !searchOpen }, modifier = Modifier.size(34.dp)) {
                    Icon(
                        if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                        "Search in book",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
        if (searchOpen) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(horizontal = 14.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Find in book…",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        "${results.size} hits",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (searchOpen && results.isNotEmpty()) {
            LazyColumn(Modifier.height(180.dp)) {
                items(results, key = { it.first }) { (idx, text) ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { state.animateScrollToItem(idx) }
                                searchOpen = false
                                query = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Text(
                            "Page ${idx + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text.take(110),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (failed) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Couldn't open this book.\nCheck your connection and try again.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items((0 until pageCount).toList(), key = { it }) { index ->
                    PageCanvas(
                        index = index,
                        ratio = pageRatio[index] ?: 0.77f,
                        bitmap = bitmaps[index],
                        colorFilter = pdfFilter(filter),
                        onTap = { zoomPage = index },
                        render = {
                            val r = renderer
                            if (r != null && index < r.pageCount) {
                                r.openPage(index).use { p ->
                                    val w = 1000
                                    val h = (w * p.height.toFloat() / p.width).toInt()
                                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                    bmp.eraseColor(android.graphics.Color.WHITE)
                                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    bitmaps[index] = bmp
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PageCanvas(
    index: Int,
    ratio: Float,
    bitmap: Bitmap?,
    colorFilter: ColorFilter?,
    onTap: () -> Unit,
    render: suspend () -> Unit,
) {
    LaunchedEffect(index, bitmap == null) {
        if (bitmap == null) withContext(Dispatchers.IO) { render() }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .clickable { onTap() },
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bmp.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                "Page ${index + 1}",
                color = Color(0xFF9C8E7A),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** Full-screen page with pinch-to-zoom and pan. */
@Composable
private fun ZoomPageDialog(
    bitmap: Bitmap,
    colorFilter: ColorFilter?,
    page: Int,
    total: Int,
    onClose: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offset = if (scale > 1f) offset + pan else Offset.Zero
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap.asImageBitmap(),
                    contentDescription = "Page $page of $total",
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bitmap.width.toFloat() / bitmap.height)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
                )
            }
            Text(
                "Page $page of $total · pinch to zoom, drag to pan",
                color = Color(0xFFB3A491),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
            )
            Surface(
                color = Color(0xCC141110),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .size(38.dp)
                    .clickable { onClose() },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, "Close page", tint = Color(0xFFF2E7D3), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PageJumpDialog(
    pageCount: Int,
    current: Int,
    onGo: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var target by remember { mutableFloatStateOf(current.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onGo(target.toInt()) }) { Text("Go to page") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Jump to a page", fontFamily = Lora, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Page ${target.toInt()} of $pageCount",
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Slider(
                    value = target,
                    onValueChange = { target = it },
                    valueRange = 1f..pageCount.toFloat(),
                    steps = (pageCount - 2).coerceAtLeast(0),
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(1, pageCount / 4, pageCount / 2, pageCount * 3 / 4, pageCount)
                        .distinct()
                        .forEach { quick ->
                            QuickPageChip("p$quick", selectedQuick = target.toInt() == quick) { target = quick.toFloat() }
                        }
                }
            }
        },
    )
}

@Composable
private fun QuickPageChip(label: String, selectedQuick: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selectedQuick) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontFamily = Inter,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selectedQuick) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun BookmarksDialog(
    marks: List<Bookmark>,
    currentPage: Int,
    onAdd: (String) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onGo: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    val here = marks.firstOrNull { it.page == currentPage }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = {
            Column {
                Text("Bookmarks", fontFamily = Lora, fontWeight = FontWeight.Bold)
                Text(
                    "Page $currentPage",
                    fontFamily = Inter,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            Column {
                if (marks.isEmpty()) {
                    Text(
                        "No bookmarks yet. Name this page below and it will be waiting for you next time.",
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                marks.forEach { mark ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onGo(mark.page) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Bookmark,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                mark.label,
                                fontFamily = Inter,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "page ${mark.page}",
                                fontFamily = Inter,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(mark) }, modifier = Modifier.size(26.dp)) {
                            Icon(
                                Icons.Default.Close,
                                "Remove bookmark",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
                Spacer(Modifier.height(14.dp))
                if (here == null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f)) {
                                if (label.isEmpty()) {
                                    Text(
                                        "Name this page",
                                        fontFamily = Inter,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                BasicTextField(
                                    value = label,
                                    onValueChange = { label = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            TextButton(
                                onClick = {
                                    onAdd(label.ifBlank { "Page $currentPage" })
                                    label = ""
                                },
                            ) { Text("Save") }
                        }
                    }
                } else {
                    Text(
                        "This page is saved as \"${here.label}\".",
                        fontFamily = Inter,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
