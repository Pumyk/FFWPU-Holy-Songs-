package com.hphc.library

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BooksScreen(remoteBooks: List<Book>, refreshing: Boolean, onOpen: (Book) -> Unit) {
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
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(book) },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (book.remote) Icons.Default.Cloud else Icons.Default.Description,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text(book.title, fontFamily = Lora, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (book.author.isNotBlank()) {
                            Text(book.author, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            if (book.remote) "From the cloud · tap to open" else book.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Native PDF reader: lazy page rendering, per-book reading position,
 * and in-book text search for bundled books.
 */
@Composable
fun PdfBookScreen(book: Book) {
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
    val prefs = remember { ctx.getSharedPreferences("pdf-progress", 0) }
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = prefs.getInt(book.id, 0).coerceAtLeast(0),
    )

    val pageTexts = remember(book.id) { Data.pageText(ctx, book.id) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

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
            prefs.edit().putInt(book.id, idx).apply()
        }
    }

    val results = remember(query, pageTexts) {
        if (query.length < 2 || pageTexts == null) emptyList()
        else pageTexts.withIndex()
            .filter { it.value.contains(query, true) }
            .take(40)
            .map { it.index to it.value }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
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
            if (pageTexts != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.clickable { searchOpen = !searchOpen },
                ) {
                    Icon(
                        if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                        "Search in book",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(16.dp),
                    )
                }
            }
        }
        if (searchOpen) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Search, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
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
private fun PageCanvas(index: Int, ratio: Float, bitmap: Bitmap?, render: suspend () -> Unit) {
    LaunchedEffect(index, bitmap == null) {
        if (bitmap == null) withContext(Dispatchers.IO) { render() }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White),
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bmp.asImageBitmap(), contentDescription = "Page ${index + 1}", modifier = Modifier.fillMaxSize())
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
