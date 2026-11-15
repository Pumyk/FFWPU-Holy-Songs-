package com.hphc.library

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Section(val label: String, val icon: ImageVector) {
    Songs("Holy Songs", Icons.Default.Book),
    Pledge("Family Pledge", Icons.Default.Handshake),
    Books("Books", Icons.Default.MenuBook),
    News("HJ Global News", Icons.Default.Newspaper),
}

sealed interface Route {
    data object Home : Route
    data class SectionRoute(val section: Section) : Route
    data class SongRoute(val song: Song) : Route
    data class BookRoute(val book: Book) : Route
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HphcTheme { App() } }
    }
}

@Composable
private fun App() {
    val ctx = LocalContext.current
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var route by remember { mutableStateOf<Route>(Route.Home) }

    // System back: song/book -> its section -> home. Only home exits the app.
    BackHandler(enabled = route !is Route.Home) {
        route = when (val r = route) {
            is Route.SongRoute -> Route.SectionRoute(Section.Songs)
            is Route.BookRoute -> Route.SectionRoute(Section.Books)
            else -> Route.Home
        }
    }

    val bundled = remember { Data.bundledSongs(ctx) }
    var remote by remember { mutableStateOf<List<Song>>(emptyList()) }
    val songs = remember(bundled, remote) { Data.mergedSongs(bundled, remote) }
    val pledge = remember { Data.bundledPledge(ctx) }
    val bundledNews = remember { Data.bundledNews(ctx) }
    var news by remember { mutableStateOf(bundledNews.first) }
    var channel by remember { mutableStateOf(bundledNews.second) }
    var newsRefreshing by remember { mutableStateOf(true) }

    // Remote content: new songs from the manifest appear without an app update.
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

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(20.dp)) {
                    Text("HPHC Library", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(
                        "Holy Songs, Pledge, Books, News",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 18.dp),
                    )
                    Section.entries.forEach { s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    route = Route.SectionRoute(s)
                                    scope.launch { drawer.close() }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(s.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(s.label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 34.dp, start = 6.dp, end = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val showBack = route !is Route.Home
                        if (showBack) {
                            IconButton(onClick = {
                                route = when (val r = route) {
                                    is Route.SongRoute -> Route.SectionRoute(Section.Songs)
                                    is Route.BookRoute -> Route.SectionRoute(Section.Books)
                                    else -> Route.Home
                                }
                            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                        } else {
                            IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Menu") }
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            (route as? Route.SectionRoute)?.section?.label ?: "HPHC Library",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
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
                when (val r = route) {
                    is Route.Home -> HomeScreen(
                        songCount = songs.size,
                        onPick = { route = Route.SectionRoute(it) },
                    )
                    is Route.SectionRoute -> when (r.section) {
                        Section.Songs -> SongsScreen(songs) { route = Route.SongRoute(it) }
                        Section.Pledge -> PledgeScreen(pledge)
                        Section.Books -> BooksScreen { route = Route.BookRoute(it) }
                        Section.News -> NewsScreen(news, channel, newsRefreshing) {
                            scope.launch {
                                newsRefreshing = true
                                val fresh = withContext(Dispatchers.IO) { Data.remoteNews() }
                                if (fresh != null) {
                                    news = fresh.first
                                    channel = fresh.second
                                }
                                newsRefreshing = false
                            }
                        }
                    }
                    is Route.SongRoute -> SongDetailScreen(r.song) { route = Route.SectionRoute(Section.Songs) }
                    is Route.BookRoute -> PdfBookScreen(r.book) { route = Route.SectionRoute(Section.Books) }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(songCount: Int, onPick: (Section) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("Welcome", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("HPHC Library", fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(
            "$songCount songs ready",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 22.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Section.entries.forEach { s ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(s) },
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(s.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(s.label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
