package com.hphc.library

import android.content.Context

/**
 * Small persistent store for the personal side of the library:
 * starred songs and the reader's preferred lyric size.
 *
 * Plain SharedPreferences on purpose - no extra dependency, instant reads,
 * and it survives app restarts without any IO thread juggling.
 */
class LibraryPrefs(ctx: Context) {

    private val sp = ctx.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

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

    /** Starred songs, newest first is not meaningful, so keep song order. */
    fun favoriteCount(): Int = favorites.size

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

    companion object {
        private const val FILE = "hphc-library"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_ZOOM = "lyric-zoom"
        private const val KEY_THEME = "theme-mode"

        const val MIN_ZOOM = 0.85f
        const val MAX_ZOOM = 1.6f
        const val STEP_ZOOM = 0.1f
    }
}
