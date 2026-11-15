package com.hphc.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
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

data class Book(val id: String, val title: String, val author: String, val asset: String, val note: String)

data class NewsItem(val title: String, val id: String, val date: String, val views: Long)

object Data {
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/Pumyk/FFWPU-Holy-Songs-/main/content/manifest.json"
    private const val NEWS_URL =
        "https://raw.githubusercontent.com/Pumyk/FFWPU-Holy-Songs-/main/public/news.json"

    fun readAsset(ctx: Context, path: String): String =
        ctx.assets.open(path).bufferedReader().use { it.readText() }

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
                    number = o.optString("number").ifBlank { null },
                    lyrics = lyrics,
                    artist = o.optString("artist").ifBlank { null },
                    album = o.optString("album").ifBlank { null },
                    category = o.optString("category").ifBlank { null },
                )
            )
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

    val books = listOf(
        Book("mother-of-peace", "Mother of Peace", "Dr. Hak Ja Han Moon", "books/mother-of-peace.pdf", "Memoir"),
        Book("way-of-tradition", "The Way of Tradition, Vol. I", "Rev. Sun Myung Moon", "books/way-of-tradition-vol1.pdf", "Selected passages"),
    )

    fun bundledNews(ctx: Context): Pair<List<NewsItem>, String> = parseNews(readAsset(ctx, "news.json"))

    private fun parseNews(raw: String): Pair<List<NewsItem>, String> {
        val o = JSONObject(raw)
        val channel = o.optJSONObject("channel")?.optString("title") ?: "HJ PeaceTV"
        val arr = o.optJSONArray("items") ?: JSONArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val it = arr.optJSONObject(i) ?: continue
                add(
                    NewsItem(
                        title = it.optString("title"),
                        id = it.optString("id"),
                        date = it.optString("date"),
                        views = it.optLong("views"),
                    )
                )
            }
        } to channel
    }

    private fun httpGet(url: String): String? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "HPHC-Library")
        conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    } catch (_: Exception) {
        null
    }

    /** Songs added to content/manifest.json appear without an app update. */
    fun remoteSongs(): List<Song> = try {
        val body = httpGet("$MANIFEST_URL?t=${System.currentTimeMillis()}") ?: return emptyList()
        parseSongs(JSONObject(body).optJSONArray("songs") ?: JSONArray())
    } catch (_: Exception) {
        emptyList()
    }

    fun mergedSongs(local: List<Song>, remote: List<Song>): List<Song> {
        val byId = LinkedHashMap<String, Song>()
        local.forEach { byId[it.id] = it }
        remote.forEach { byId[it.id] = it }
        return byId.values.toList()
    }

    fun remoteNews(): Pair<List<NewsItem>, String>? = try {
        val body = httpGet("$NEWS_URL?t=${System.currentTimeMillis()}") ?: return null
        parseNews(body).takeIf { it.first.isNotEmpty() }
    } catch (_: Exception) {
        null
    }

    fun youtubeUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"
}
