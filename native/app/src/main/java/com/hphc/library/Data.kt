package com.hphc.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Song(
    val id: String,
    val title: String,
    val number: String?,
    val lyrics: String,
    val artist: String?,
    val album: String?,
    val category: String?,
)

data class PledgeVerse(val number: String, val text: String)

/** A book on the shelf: bundled (asset != null) or remote (remoteUrl != null). */
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val note: String,
    val asset: String? = null,
    val remoteUrl: String? = null,
    val textAsset: String? = null,
) {
    val remote: Boolean get() = remoteUrl != null

    /** True for books that open as a structured chapter reader, not a PDF. */
    val structured: Boolean get() = textAsset != null
}

data class NewsItem(val title: String, val id: String, val date: String, val views: Long)

object Data {
    private const val BASE_RAW =
        "https://raw.githubusercontent.com/Pumyk/FFWPU-Holy-Songs-/main/"
    private const val MANIFEST_URL = BASE_RAW + "content/manifest.json"
    private const val NEWS_URL = BASE_RAW + "public/news.json"

    fun readAsset(ctx: Context, path: String): String =
        ctx.assets.open(path).bufferedReader().use { it.readText() }

    fun httpGet(url: String): String? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 12000
        conn.setRequestProperty("User-Agent", "HPHCLibrary")
        try {
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        null
    }

    private fun JSONObject.textOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key)
        return if (v.isBlank() || v == "null") null else v
    }

    fun bundledSongs(ctx: Context): List<Song> = parseSongs(JSONArray(readAsset(ctx, "songs.json")))

    fun parseSongs(arr: JSONArray): List<Song> = buildList {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title")
            val lyrics = o.optString("lyrics")
            if (title.isBlank() || lyrics.isBlank()) continue
            add(
                Song(
                    id = o.optString("id", title.lowercase().replace(' ', '-')),
                    title = title,
                    number = o.textOrNull("number"),
                    lyrics = lyrics,
                    artist = o.textOrNull("artist"),
                    album = o.textOrNull("album"),
                    category = o.textOrNull("category"),
                )
            )
        }
    }

    fun mergedSongs(bundled: List<Song>, remote: List<Song>): List<Song> {
        if (remote.isEmpty()) return bundled
        val remoteIds = remote.map { it.id }.toSet()
        val local = bundled.filter { it.id !in remoteIds }
        return local + remote
    }

    fun remoteSongs(): List<Song> {
        val json = httpGet(MANIFEST_URL) ?: return emptyList()
        return try {
            val arr = JSONObject(json).optJSONArray("songs") ?: return emptyList()
            parseSongs(arr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun bundledPledge(ctx: Context): List<PledgeVerse> {
        val arr = JSONArray(readAsset(ctx, "pledge.json"))
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(PledgeVerse(o.optString("number"), o.optString("text")))
            }
        }
    }

    /** Books bundled with the app (offline from first launch). */
    val bundledBooks = listOf(
        Book(
            id = "mother-of-peace",
            title = "Mother of Peace",
            author = "Dr. Hak Ja Han Moon",
            note = "Memoir · 369 pages",
            asset = "books/mother-of-peace.pdf",
        ),
        Book(
            id = "way-of-tradition",
            title = "The Way of Tradition, Vol. I",
            author = "Rev. Sun Myung Moon",
            note = "Selected passages · 315 pages",
            asset = "books/way-of-tradition-vol1.pdf",
        ),
        Book(
            id = "exposition",
            title = "Exposition of the Divine Principle",
            author = "Hyo Won Eu",
            note = "17 chapters · searchable text",
            textAsset = "books/exposition.json",
        ),
    )

    /** Extra PDF books published in content/manifest.json (downloaded on first open). */
    fun remoteBooks(): List<Book> {
        val json = httpGet(MANIFEST_URL) ?: return emptyList()
        return try {
            val arr = JSONObject(json).optJSONArray("books") ?: return emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val title = o.optString("title")
                    val url = o.optString("url")
                    if (title.isBlank() || url.isBlank()) continue
                    add(
                        Book(
                            id = o.optString("id", "book-$i"),
                            title = title,
                            author = o.optString("author", ""),
                            note = o.optString("note", "From the cloud"),
                            remoteUrl = if (url.startsWith("http")) url else BASE_RAW + url.removePrefix("/"),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Local PDF file for a book: copies bundled assets, downloads remote ones once. */
    fun pdfFile(ctx: Context, book: Book): File? {
        val dir = File(ctx.filesDir, "books").apply { mkdirs() }
        return if (book.remoteUrl != null) {
            val out = File(dir, book.id + ".pdf")
            if (out.exists() && out.length() > 0) out
            else try {
                val conn = URL(book.remoteUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 60000
                conn.setRequestProperty("User-Agent", "HPHCLibrary")
                try {
                    if (conn.responseCode != 200) return null
                    conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
                    out
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                null
            }
        } else {
            val asset = book.asset ?: return null
            val out = File(ctx.cacheDir, asset.replace('/', '_'))
            if (!out.exists() || out.length() == 0L) {
                ctx.assets.open(asset).use { input -> out.outputStream().use { input.copyTo(it) } }
            }
            out
        }
    }

    /** Per-page text for in-book search (bundled books only). */
    fun pageText(ctx: Context, bookId: String): List<String>? = try {
        val arr = JSONArray(readAsset(ctx, "books/$bookId.text.json"))
        List(arr.length()) { arr.optString(it, "") }
    } catch (e: Exception) {
        null
    }

    fun youtubeUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"

    /** True when the device currently has a usable internet connection. */
    fun isOnline(ctx: Context): Boolean {
        val cm = ctx.getSystemService(android.net.ConnectivityManager::class.java) ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Cached thumbnail for a news edition. Fetched once while online, then
     * reused so an offline tap still shows a recognisable cover.
     */
    fun thumbFile(ctx: Context, videoId: String): File? {
        if (videoId.isBlank()) return null
        val dir = File(ctx.filesDir, "thumbs").apply { mkdirs() }
        val out = File(dir, "$videoId.jpg")
        if (out.exists() && out.length() > 0) return out
        if (!isOnline(ctx)) return null
        return try {
            val conn = URL("https://i.ytimg.com/vi/$videoId/hqdefault.jpg")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "HPHCLibrary")
            try {
                if (conn.responseCode != 200) return null
                conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
                out.takeIf { it.length() > 0 }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun bundledNews(ctx: Context): Pair<List<NewsItem>, String> = parseNews(readAsset(ctx, "news.json"))

    fun remoteNews(): Pair<List<NewsItem>, String>? {
        val json = httpGet(NEWS_URL) ?: return null
        return try {
            parseNews(json).takeIf { it.first.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNews(raw: String): Pair<List<NewsItem>, String> {
        val root = JSONObject(raw)
        val channel = root.optJSONObject("channel")?.optString("title").orEmpty().ifBlank { "HJ PeaceTV" }
        val arr = root.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    NewsItem(
                        title = o.optString("title"),
                        id = o.optString("id"),
                        date = o.optString("date"),
                        views = o.optLong("views", 0),
                    )
                )
            }
        }
        return items to channel
    }

    /** Structured, searchable text book (currently the Divine Principle). */
    fun structuredBook(ctx: Context, asset: String): StructuredBook? = try {
        val root = JSONObject(readAsset(ctx, asset))
        val chapters = root.optJSONArray("chapters") ?: JSONArray()
        val list = buildList {
            for (i in 0 until chapters.length()) {
                val c = chapters.optJSONObject(i) ?: continue
                val blocks = c.optJSONArray("blocks") ?: JSONArray()
                add(
                    Chapter(
                        id = c.optString("id", "ch-$i"),
                        title = c.optString("title", "Chapter ${i + 1}"),
                        blocks = buildList {
                            for (j in 0 until blocks.length()) {
                                val b = blocks.optJSONObject(j) ?: continue
                                val text = b.optString("text").trim()
                                if (text.isNotEmpty()) {
                                    add(DocBlock(b.optString("type", "p"), text))
                                }
                            }
                        },
                    )
                )
            }
        }
        StructuredBook(
            title = root.optString("title", "Exposition of the Divine Principle"),
            author = root.optString("author", ""),
            source = root.optString("source", ""),
            chapters = list,
        )
    } catch (e: Exception) {
        null
    }
}

data class DocBlock(val type: String, val text: String)

data class Chapter(val id: String, val title: String, val blocks: List<DocBlock>) {
    val wordCount: Int get() = blocks.sumOf { it.text.split(' ').size }
}

data class StructuredBook(
    val title: String,
    val author: String,
    val source: String,
    val chapters: List<Chapter>,
)
