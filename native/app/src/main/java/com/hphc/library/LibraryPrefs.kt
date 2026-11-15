package com.hphc.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small persistent store for the personal side of the library:
 * starred songs, service sets, what was read last, and the reading
 * preferences that should follow the reader from screen to screen.
 *
 * Plain SharedPreferences on purpose - no extra dependency, instant reads,
 * and it survives app restarts without any IO thread juggling.
 */
class LibraryPrefs(ctx: Context) {

    private val app = ctx.applicationContext
    private val sp = app.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Reading position, page counts and bookmarks for the PDF shelf. */
    private val pdf = app.getSharedPreferences(PDF_FILE, Context.MODE_PRIVATE)

    /* ---------------------------------------------------------------- *
     * Songs
     * ---------------------------------------------------------------- */

    val favorites: Set<String>
        get() = sp.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun isFavorite(songId: String): Boolean = favorites.contains(songId)

    /** Adds or removes the song, persists, and returns the updated set. */
    fun toggleFavorite(songId: String): Set<String> {
        val next = favorites.toMutableSet()
        if (!next.add(songId)) next.remove(songId)
        sp.edit().putStringSet(KEY_FAVORITES, next).apply()
        return next
    }

    fun favoriteCount(): Int = favorites.size

    /** Recently opened songs, newest first. */
    fun recents(): List<String> =
        (sp.getString(KEY_RECENTS, "") ?: "")
            .split('\u0001')
            .filter { it.isNotBlank() }

    fun pushRecent(songId: String) {
        val next = (listOf(songId) + recents().filter { it != songId }).take(RECENTS_MAX)
        sp.edit().putString(KEY_RECENTS, next.joinToString("\u0001")).apply()
    }

    /* ---------------------------------------------------------------- *
     * Service sets (custom playlists such as "Sunday Service")
     * ---------------------------------------------------------------- */

    fun playlists(): Map<String, Set<String>> {
        val raw = sp.getString(KEY_PLAYLISTS, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            buildMap {
                root.keys().forEach { name ->
                    val arr = root.optJSONArray(name) ?: JSONArray()
                    put(name, List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }.toSet())
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun savePlaylists(sets: Map<String, Set<String>>) {
        val root = JSONObject()
        sets.forEach { (name, ids) -> root.put(name, JSONArray(ids.toList())) }
        sp.edit().putString(KEY_PLAYLISTS, root.toString()).apply()
    }

    /** Creates an empty set; returns false when the name is already taken. */
    fun createPlaylist(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val sets = playlists().toMutableMap()
        if (sets.keys.any { it.equals(trimmed, true) }) return false
        sets[trimmed] = emptySet()
        savePlaylists(sets)
        return true
    }

    fun deletePlaylist(name: String) {
        val sets = playlists().toMutableMap()
        sets.remove(name)
        savePlaylists(sets)
    }

    fun addToPlaylist(name: String, songId: String) {
        val sets = playlists().toMutableMap()
        sets[name] = (sets[name] ?: emptySet()) + songId
        savePlaylists(sets)
    }

    fun removeFromPlaylist(name: String, songId: String) {
        val sets = playlists().toMutableMap()
        sets[name] = (sets[name] ?: emptySet()) - songId
        savePlaylists(sets)
    }

    /* ---------------------------------------------------------------- *
     * Reading experience
     * ---------------------------------------------------------------- */

    /** System / Light / Dark / AMOLED, chosen in the nav drawer. */
    var themeMode: ThemeMode
        get() = ThemeMode.fromName(sp.getString(KEY_THEME, ThemeMode.System.name))
        set(value) {
            sp.edit().putString(KEY_THEME, value.name).apply()
        }

    var lyricZoom: Float
        get() = sp.getFloat(KEY_ZOOM, 1f).coerceIn(MIN_ZOOM, MAX_ZOOM)
        set(value) {
            sp.edit().putFloat(KEY_ZOOM, value.coerceIn(MIN_ZOOM, MAX_ZOOM)).apply()
        }

    /** Keep the screen awake while lyrics or the pledge are open. */
    var keepAwake: Boolean
        get() = sp.getBoolean(KEY_AWAKE, false)
        set(value) {
            sp.edit().putBoolean(KEY_AWAKE, value).apply()
        }

    /** Hands-free scrolling while singing. 0 means off. */
    var autoScrollSpeed: Int
        get() = sp.getInt(KEY_SCROLL, 0).coerceIn(0, MAX_SCROLL)
        set(value) {
            sp.edit().putInt(KEY_SCROLL, value.coerceIn(0, MAX_SCROLL)).apply()
        }

    /** Hide repeated chorus blocks so a song fits one screen. */
    var hideChorus: Boolean
        get() = sp.getBoolean(KEY_HIDE_CHORUS, false)
        set(value) {
            sp.edit().putBoolean(KEY_HIDE_CHORUS, value).apply()
        }

    /** Pledge typeface: false = Lora serif, true = Plus Jakarta Sans. */
    var pledgeSans: Boolean
        get() = sp.getBoolean(KEY_PLEDGE_SANS, false)
        set(value) {
            sp.edit().putBoolean(KEY_PLEDGE_SANS, value).apply()
        }

    /** 0 = off, 1 = night, 2 = warm paper. */
    var pdfFilter: Int
        get() = sp.getInt(KEY_PDF_FILTER, 0).coerceIn(0, 2)
        set(value) {
            sp.edit().putInt(KEY_PDF_FILTER, value.coerceIn(0, 2)).apply()
        }

    /* ---------------------------------------------------------------- *
     * News
     * ---------------------------------------------------------------- */

    val watchedNews: Set<String>
        get() = sp.getStringSet(KEY_WATCHED, emptySet())?.toSet() ?: emptySet()

    fun markWatched(id: String) {
        val next = watchedNews + id
        sp.edit().putStringSet(KEY_WATCHED, next).apply()
    }

    /* ---------------------------------------------------------------- *
     * PDF reading state
     * ---------------------------------------------------------------- */

    fun pdfPage(bookId: String): Int = pdf.getInt("page-$bookId", 0).coerceAtLeast(0)

    fun setPdfPage(bookId: String, page: Int) {
        pdf.edit().putInt("page-$bookId", page.coerceAtLeast(0)).apply()
    }

    fun pdfTotal(bookId: String): Int = pdf.getInt("total-$bookId", 0).coerceAtLeast(0)

    fun setPdfTotal(bookId: String, total: Int) {
        if (total > 0) pdf.edit().putInt("total-$bookId", total).apply()
    }

    /** Named bookmarks for a book, in page order. */
    fun bookmarks(bookId: String): List<Bookmark> {
        val raw = pdf.getString("marks-$bookId", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(Bookmark(o.optInt("page", 0), o.optString("label", "Bookmark")))
                }
            }.sortedBy { it.page }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveBookmarks(bookId: String, marks: List<Bookmark>) {
        val arr = JSONArray()
        marks.sortedBy { it.page }.forEach {
            arr.put(JSONObject().put("page", it.page).put("label", it.label))
        }
        pdf.edit().putString("marks-$bookId", arr.toString()).apply()
    }

    companion object {
        private const val FILE = "hphc-library"
        private const val PDF_FILE = "pdf-progress"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_ZOOM = "lyric-zoom"
        private const val KEY_THEME = "theme-mode"
        private const val KEY_RECENTS = "recent-songs"
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_AWAKE = "keep-awake"
        private const val KEY_SCROLL = "auto-scroll"
        private const val KEY_HIDE_CHORUS = "hide-chorus"
        private const val KEY_PLEDGE_SANS = "pledge-sans"
        private const val KEY_PDF_FILTER = "pdf-filter"
        private const val KEY_WATCHED = "watched-news"

        private const val RECENTS_MAX = 12

        const val MIN_ZOOM = 0.85f
        const val MAX_ZOOM = 1.6f
        const val STEP_ZOOM = 0.1f

        const val MAX_SCROLL = 10
    }
}

/** A saved place in a PDF book. */
data class Bookmark(val page: Int, val label: String)
