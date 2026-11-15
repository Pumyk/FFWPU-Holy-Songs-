package com.hphc.library

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Section(val label: String, val blurb: String, val icon: ImageVector) {
    Songs("Holy Songs", "42 hymns, searchable", Icons.Default.Book),
    Pledge("Family Pledge", "Eight pledges", Icons.Default.Handshake),
    Books("Books", "Full texts, offline", Icons.Default.MenuBook),
    News("HJ Global News", "Weekly editions", Icons.Default.Newspaper),
}

sealed interface Route {
    data object Home : Route
    data class SectionRoute(val section: Section) : Route
    data class SongRoute(val song: Song) : Route
    data class BookRoute(val book: Book) : Route
    data class VideoRoute(val item: NewsItem) : Route
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = LibraryPrefs(this)
        setContent {
            var themeMode by remember { mutableStateOf(prefs.themeMode) }
            HphcTheme(themeMode) {
                App(
                    prefs = prefs,
                    themeMode = themeMode,
                    onThemeMode = {
                        themeMode = it
                        prefs.themeMode = it
                    },
                )
            }
        }
    }
}

@Composable
private fun App(
    prefs: LibraryPrefs,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
) {
    val ctx = LocalContext.current
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var route by remember { mutableStateOf<Route>(Route.Home) }
    var favorites by remember { mutableStateOf(prefs.favorites) }
    var lyricZoom by remember { mutableFloatStateOf(prefs.lyricZoom) }
    var keepAwake by remember { mutableStateOf(prefs.keepAwake) }
    var autoSpeed by remember { mutableIntStateOf(prefs.autoScrollSpeed) }
    var hideChorus by remember { mutableStateOf(prefs.hideChorus) }
    var pledgeSans by remember { mutableStateOf(prefs.pledgeSans) }
    var recents by remember { mutableStateOf(prefs.recents()) }
    var playlists by remember { mutableStateOf(prefs.playlists()) }
    var watched by remember { mutableStateOf(prefs.watchedNews) }
    var wideSong by remember { mutableStateOf<Song?>(null) }

    val bundled = remember { Data.bundledSongs(ctx) }
    var remote by remember { mutableStateOf<List<Song>>(emptyList()) }
    val songs = remember(bundled, remote) { Data.mergedSongs(bundled, remote) }
    val pledge = remember { Data.bundledPledge(ctx) }
    val bundledNews = remember { Data.bundledNews(ctx) }
    var news by remember { mutableStateOf(bundledNews.first) }
    var channel by remember { mutableStateOf(bundledNews.second) }
    var newsRefreshing by remember { mutableStateOf(true) }

    var remoteBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var shelfRefreshing by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val r = withContext(Dispatchers.IO) { Data.remoteSongs() }
        if (r.isNotEmpty()) remote = r
    }
    LaunchedEffect(Unit) {
        val fresh = withContext(Dispatchers.IO) { Data.remoteNews() }
        if (fresh != null) {
            news = fresh.first
            channel = fresh.second
        }
        newsRefreshing = false
    }
    LaunchedEffect(Unit) {
        remoteBooks = withContext(Dispatchers.IO) { Data.remoteBooks() }
        shelfRefreshing = false
    }

    BoxWithConstraints {
        val wide = maxWidth >= 720.dp

        /** Opens a song: remembers it and routes to it (or the wide side pane). */
        val choose: (Song) -> Unit = { song ->
            prefs.pushRecent(song.id)
            recents = prefs.recents()
            if (wide) wideSong = song else route = Route.SongRoute(song)
        }

        BackHandler(enabled = route !is Route.Home) {
            route = when (val r = route) {
                is Route.SongRoute -> Route.SectionRoute(Section.Songs)
                is Route.BookRoute -> Route.SectionRoute(Section.Books)
                is Route.VideoRoute -> Route.SectionRoute(Section.News)
                else -> Route.Home
            }
        }

        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = !wide,
            drawerContent = {
                // Slide-over panel: 78% width, so the home screen stays visible behind it.
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.78f),
                    drawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 30.dp)) {
                        Text("HPHC Library", fontFamily = Lora, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Holy Songs · Pledge · Books · News",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 22.dp),
                        )
                        Section.entries.forEach { s ->
                            val active = (route as? Route.SectionRoute)?.section == s
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        route = Route.SectionRoute(s)
                                        scope.launch { drawer.close() }
                                    }
                                    .background(
                                        if (active) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        RoundedCornerShape(14.dp),
                                    )
                                    .padding(horizontal = 12.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    s.icon, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(s.label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        s.blurb,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        Spacer(Modifier.height(26.dp))
                        Text(
                            "APPEARANCE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(9.dp))
                        // Two rows of two: four pills do not fit one row in a 78% drawer.
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ThemePill(ThemeMode.System.label, themeMode == ThemeMode.System) {
                                    onThemeMode(ThemeMode.System)
                                }
                                ThemePill(ThemeMode.Light.label, themeMode == ThemeMode.Light) {
                                    onThemeMode(ThemeMode.Light)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ThemePill(ThemeMode.Dark.label, themeMode == ThemeMode.Dark) {
                                    onThemeMode(ThemeMode.Dark)
                                }
                                ThemePill(ThemeMode.Amoled.label, themeMode == ThemeMode.Amoled) {
                                    onThemeMode(ThemeMode.Amoled)
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "READING",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ThemePill("Screen on", keepAwake) {
                                keepAwake = !keepAwake
                                prefs.keepAwake = keepAwake
                            }
                            ThemePill(
                                if (autoSpeed == 0) "Auto-scroll off" else "Auto ×$autoSpeed",
                                autoSpeed > 0,
                            ) {
                                autoSpeed = if (autoSpeed == 0) 4 else 0
                                prefs.autoScrollSpeed = autoSpeed
                            }
                        }
                    }
                }
            },
        ) {
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            IconButton(onClick = { route = Route.Home }) {
                                Text(
                                    "H",
                                    fontFamily = Lora,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    ) {
                        Spacer(Modifier.height(6.dp))
                        NavigationRailItem(
                            selected = route is Route.Home,
                            onClick = { route = Route.Home },
                            icon = { Icon(Icons.Default.Home, "Home") },
                            label = { Text("Home", fontSize = 9.sp) },
                        )
                        Section.entries.forEach { s ->
                            NavigationRailItem(
                                selected = (route as? Route.SectionRoute)?.section == s,
                                onClick = { route = Route.SectionRoute(s) },
                                icon = { Icon(s.icon, s.label) },
                                label = {
                                    Text(
                                        s.label.substringBefore(' '),
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                },
                            )
                        }
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (route is Route.Home && !wide) {
                                    IconButton(onClick = { scope.launch { drawer.open() } }) {
                                        Icon(Icons.Default.Menu, "Open menu")
                                    }
                                } else if (route !is Route.Home) {
                                    IconButton(onClick = {
                                        route = when (val r = route) {
                                            is Route.SongRoute -> Route.SectionRoute(Section.Songs)
                                            is Route.BookRoute -> Route.SectionRoute(Section.Books)
                                            is Route.VideoRoute -> Route.SectionRoute(Section.News)
                                            else -> Route.Home
                                        }
                                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                                } else {
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    when (val r = route) {
                                        is Route.Home -> "HPHC Library"
                                        is Route.SectionRoute -> r.section.label
                                        is Route.SongRoute -> "Holy Songs"
                                        is Route.BookRoute -> "Books"
                                        is Route.VideoRoute -> "HJ Global News"
                                    },
                                    fontFamily = Lora,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    },
                ) { padding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        AnimatedContent(
                            targetState = route,
                            transitionSpec = {
                                (fadeIn(tween(170)) + slideInHorizontally(tween(170)) { it / 14 })
                                    .togetherWith(fadeOut(tween(90)))
                            },
                            label = "route",
                        ) { r ->
                            when (r) {
                                is Route.Home -> HomeScreen(
                                    onPick = { route = Route.SectionRoute(it) },
                                )
                                is Route.SectionRoute -> when (r.section) {
                                    Section.Songs -> {
                                        val list: @Composable (Modifier) -> Unit = { m ->
                                            SongsScreen(
                                                songs = songs,
                                                favorites = favorites,
                                                recentIds = recents,
                                                playlists = playlists,
                                                onToggleFavorite = { favorites = prefs.toggleFavorite(it) },
                                                onOpen = choose,
                                                onRemoveFromSet = { name, id ->
                                                    prefs.removeFromPlaylist(name, id)
                                                    playlists = prefs.playlists()
                                                },
                                                selectedId = if (wide) wideSong?.id else null,
                                                modifier = m,
                                            )
                                        }
                                        if (wide) {
                                            Row(Modifier.fillMaxSize()) {
                                                list(Modifier.weight(0.44f))
                                                Box(Modifier.weight(0.56f)) {
                                                    val song = wideSong
                                                    if (song == null) {
                                                        Box(
                                                            Modifier
                                                                .fillMaxSize()
                                                                .padding(30.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Text(
                                                                "Pick a hymn to read it here.",
                                                                fontFamily = Lora,
                                                                fontSize = 15.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                textAlign = TextAlign.Center,
                                                            )
                                                        }
                                                    } else {
                                                        SongPage(
                                                            song = song,
                                                            songs = songs,
                                                            isFavorite = favorites.contains(song.id),
                                                            onToggleFavorite = {
                                                                favorites = prefs.toggleFavorite(song.id)
                                                            },
                                                            zoom = lyricZoom,
                                                            onZoom = {
                                                                lyricZoom = it
                                                                prefs.lyricZoom = it
                                                            },
                                                            hideChorus = hideChorus,
                                                            onToggleChorus = {
                                                                hideChorus = !hideChorus
                                                                prefs.hideChorus = hideChorus
                                                            },
                                                            keepAwake = keepAwake,
                                                            onToggleKeepAwake = {
                                                                keepAwake = !keepAwake
                                                                prefs.keepAwake = keepAwake
                                                            },
                                                            autoSpeed = autoSpeed,
                                                            onAutoSpeed = {
                                                                autoSpeed = it
                                                                prefs.autoScrollSpeed = it
                                                            },
                                                            playlists = playlists,
                                                            onAddToSet = { name, id ->
                                                                prefs.addToPlaylist(name, id)
                                                                playlists = prefs.playlists()
                                                            },
                                                            onCreateSet = { name, id ->
                                                                prefs.createPlaylist(name)
                                                                prefs.addToPlaylist(name.trim(), id)
                                                                playlists = prefs.playlists()
                                                            },
                                                            onOpenSong = choose,
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            list(Modifier)
                                        }
                                    }
                                    Section.Pledge -> PledgeScreen(
                                        verses = pledge,
                                        zoom = lyricZoom,
                                        onZoom = {
                                            lyricZoom = it
                                            prefs.lyricZoom = it
                                        },
                                        sans = pledgeSans,
                                        onSans = {
                                            pledgeSans = it
                                            prefs.pledgeSans = it
                                        },
                                        keepAwake = keepAwake,
                                        onToggleKeepAwake = {
                                            keepAwake = !keepAwake
                                            prefs.keepAwake = keepAwake
                                        },
                                        autoSpeed = autoSpeed,
                                        onAutoSpeed = {
                                            autoSpeed = it
                                            prefs.autoScrollSpeed = it
                                        },
                                    )
                                    Section.Books -> BooksScreen(
                                        remoteBooks = remoteBooks,
                                        refreshing = shelfRefreshing,
                                        prefs = prefs,
                                        onOpen = { route = Route.BookRoute(it) },
                                    )
                                    Section.News -> NewsScreen(
                                        items = news,
                                        channel = channel,
                                        refreshing = newsRefreshing,
                                        watched = watched,
                                        onRefresh = {
                                            scope.launch {
                                                newsRefreshing = true
                                                val fresh = withContext(Dispatchers.IO) { Data.remoteNews() }
                                                if (fresh != null) {
                                                    news = fresh.first
                                                    channel = fresh.second
                                                }
                                                newsRefreshing = false
                                            }
                                        },
                                        onPlay = {
                                            prefs.markWatched(it.id)
                                            watched = prefs.watchedNews
                                            route = Route.VideoRoute(it)
                                        },
                                    )
                                }
                                is Route.SongRoute -> SongPage(
                                    song = r.song,
                                    songs = songs,
                                    isFavorite = favorites.contains(r.song.id),
                                    onToggleFavorite = {
                                        favorites = prefs.toggleFavorite(r.song.id)
                                    },
                                    zoom = lyricZoom,
                                    onZoom = {
                                        lyricZoom = it
                                        prefs.lyricZoom = it
                                    },
                                    hideChorus = hideChorus,
                                    onToggleChorus = {
                                        hideChorus = !hideChorus
                                        prefs.hideChorus = hideChorus
                                    },
                                    keepAwake = keepAwake,
                                    onToggleKeepAwake = {
                                        keepAwake = !keepAwake
                                        prefs.keepAwake = keepAwake
                                    },
                                    autoSpeed = autoSpeed,
                                    onAutoSpeed = {
                                        autoSpeed = it
                                        prefs.autoScrollSpeed = it
                                    },
                                    playlists = playlists,
                                    onAddToSet = { name, id ->
                                        prefs.addToPlaylist(name, id)
                                        playlists = prefs.playlists()
                                    },
                                    onCreateSet = { name, id ->
                                        prefs.createPlaylist(name)
                                        prefs.addToPlaylist(name.trim(), id)
                                        playlists = prefs.playlists()
                                    },
                                    onOpenSong = choose,
                                )
                                is Route.BookRoute -> if (r.book.structured) {
                                    ExpositionScreen(r.book)
                                } else {
                                    PdfBookScreen(r.book, prefs)
                                }
                                is Route.VideoRoute -> YouTubeScreen(r.item)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One song page, wired for whichever list it was opened from: position and
 * prev/next come from the current song list, so stepping through the hymn
 * book works from the normal list and the wide side pane alike.
 */
@Composable
private fun SongPage(
    song: Song,
    songs: List<Song>,
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
    onOpenSong: (Song) -> Unit,
) {
    val index = songs.indexOfFirst { it.id == song.id }
    SongDetailScreen(
        song = song,
        position = if (index >= 0) index else 0,
        total = songs.size,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        zoom = zoom,
        onZoom = onZoom,
        hideChorus = hideChorus,
        onToggleChorus = onToggleChorus,
        keepAwake = keepAwake,
        onToggleKeepAwake = onToggleKeepAwake,
        autoSpeed = autoSpeed,
        onAutoSpeed = onAutoSpeed,
        playlists = playlists,
        onAddToSet = onAddToSet,
        onCreateSet = onCreateSet,
        onPrev = if (index > 0) {
            { onOpenSong(songs[index - 1]) }
        } else null,
        onNext = if (index in 0 until songs.size - 1) {
            { onOpenSong(songs[index + 1]) }
        } else null,
    )
}

@Composable
private fun HomeScreen(onPick: (Section) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            "Welcome back",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "HPHC Library",
            fontFamily = Lora,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 43.sp,
            letterSpacing = (-0.6).sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 24.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Section.entries.forEach { s ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(s) },
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    s.icon, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.label,
                                fontFamily = Lora,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.1).sp,
                            )
                            Text(
                                s.blurb,
                                fontFamily = Inter,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
