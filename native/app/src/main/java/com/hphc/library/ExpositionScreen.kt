package com.hphc.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Exposition of the Divine Principle as a structured, searchable text book:
 * a chapter list with full-text search, and a reader with proper headings
 * instead of a scanned page image.
 */
@Composable
fun ExpositionScreen(book: Book, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var data by remember { mutableStateOf<StructuredBook?>(null) }
    var loading by remember { mutableStateOf(true) }
    var chapter by remember { mutableStateOf<Chapter?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(book.id) {
        val asset = book.textAsset
        data = if (asset == null) null else withContext(Dispatchers.IO) {
            Data.structuredBook(ctx, asset)
        }
        loading = false
    }

    BackHandler(enabled = chapter != null) { chapter = null }

    val open = chapter
    val text = data
    when {
        loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        text == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Couldn't open this book.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        open != null -> ChapterReader(open, text, modifier)
        else -> ChapterList(
            book = text,
            query = query,
            onQuery = { query = it },
            onOpen = { chapter = it },
            modifier = modifier,
        )
    }
}

@Composable
private fun ChapterList(
    book: StructuredBook,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (Chapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hits = remember(query, book) {
        if (query.length < 2) emptyList()
        else book.chapters.mapNotNull { ch ->
            val hit = ch.blocks.firstOrNull { it.text.contains(query, true) }
            when {
                ch.title.contains(query, true) ->
                    ChapterHit(ch, "in the chapter title", 0)
                hit != null -> ChapterHit(ch, snippet(hit.text, query), countMentions(ch, query))
                else -> null
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(book.title, fontFamily = Lora, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
            if (book.author.isNotBlank()) {
                Text(
                    book.author,
                    fontFamily = Ui,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Text(
                "${book.chapters.size} chapters · offline",
                fontFamily = Ui,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(12.dp))
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
                        if (query.isEmpty()) {
                            Text(
                                "Search the whole book",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQuery,
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (query.length >= 2) {
                Text(
                    "${hits.sumOf { it.mentions }} matches in ${hits.size} chapters",
                    fontFamily = Ui,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        if (query.length >= 2) {
            items(hits, key = { "hit-${it.chapter.id}" }) { hit ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(hit.chapter) },
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text(
                            hit.chapter.title,
                            fontFamily = Lora,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 19.sp,
                        )
                        Text(
                            hit.preview,
                            fontFamily = Ui,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
        } else {
            items(book.chapters, key = { it.id }) { ch ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(ch) }
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            ch.title,
                            fontFamily = Lora,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 21.sp,
                        )
                        Text(
                            "${ch.blocks.size} passages",
                            fontFamily = Ui,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    Text(
                        "›",
                        fontFamily = Lora,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

private data class ChapterHit(val chapter: Chapter, val preview: String, val mentions: Int)

private fun countMentions(chapter: Chapter, query: String): Int =
    chapter.blocks.count { it.text.contains(query, true) }

private fun snippet(text: String, query: String): String {
    val at = text.indexOf(query, ignoreCase = true)
    if (at < 0) return text.take(120)
    val start = (at - 40).coerceAtLeast(0)
    val end = (at + query.length + 90).coerceAtMost(text.length)
    return buildString {
        if (start > 0) append("… ")
        append(text.substring(start, end).trim())
        if (end < text.length) append(" …")
    }
}

@Composable
private fun ChapterReader(chapter: Chapter, book: StructuredBook, modifier: Modifier = Modifier) {
    var scale by remember(chapter.id) { mutableFloatStateOf(1f) }
    val body = 17f * scale
    val leading = body * 1.72f

    Column(
        modifier
            .fillMaxSize()
            .pinchToZoom { factor -> scale = (scale * factor).coerceIn(0.85f, 1.8f) },
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            Text(
                book.title.uppercase(),
                fontFamily = Ui,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                chapter.title,
                fontFamily = Lora,
                fontSize = 23.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(chapter.blocks.size, key = { it }) { i ->
                val block = chapter.blocks[i]
                when (block.type) {
                    "h1" -> Text(
                        block.text,
                        fontFamily = Lora,
                        fontSize = (body + 6).sp,
                        lineHeight = (leading + 6).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                    )
                    "h2" -> Text(
                        block.text,
                        fontFamily = Lora,
                        fontSize = (body + 2).sp,
                        lineHeight = (leading + 2).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                    )
                    else -> Text(
                        block.text,
                        fontFamily = Lora,
                        fontSize = body.sp,
                        lineHeight = leading.sp,
                        letterSpacing = 0.1.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
