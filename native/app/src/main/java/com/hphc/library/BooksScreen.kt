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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BooksScreen(onOpen: (Book) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Books", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(
                "Full texts, offline",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        items(Data.books) { book ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(book) },
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.size(14.dp))
                    Column {
                        Text(book.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(book.author, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(book.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/**
 * Native PDF reader: vertical page scroll, pages rendered lazily with
 * PdfRenderer, reading position remembered per book.
 */
@Composable
fun PdfBookScreen(book: Book, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    val pageRatio = remember { mutableStateMapOf<Int, Float>() }
    val bitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val prefs = remember { ctx.getSharedPreferences("pdf-progress", 0) }
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = prefs.getInt(book.id, 0).coerceAtLeast(0),
    )

    DisposableEffect(book.id) {
        val f = File(ctx.cacheDir, book.asset.replace('/', '_'))
        if (!f.exists() || f.length() == 0L) {
            ctx.assets.open(book.asset).use { input -> f.outputStream().use { input.copyTo(it) } }
        }
        val descriptor = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        val r = PdfRenderer(descriptor)
        pfd = descriptor
        renderer = r
        pageCount = r.pageCount
        for (i in 0 until r.pageCount) {
            r.openPage(i).use { p ->
                pageRatio[i] = p.width.toFloat() / p.height.toFloat()
            }
        }
        onDispose {
            bitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
            renderer?.close()
            pfd?.close()
        }
    }

    LaunchedEffect(book.id) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { idx ->
            prefs.edit().putInt(book.id, idx).apply()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "page ${state.firstVisibleItemIndex + 1} / $pageCount",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(state = state, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
