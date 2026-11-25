package com.hphc.library

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val PLAYER_TITLE_PREFIX = "HPHC_PLAYER:"

private enum class Playback {
    Loading, Ready, Playing, Paused, Ended, Restricted, Unavailable, Failed,
}

/**
 * Use the official IFrame API with the installed Android app ID as the HTTPS
 * referrer, including the debug variant. The local wrapper owns the document
 * title; only this title (not an Android JS bridge) carries playback status.
 */
private fun playerHtml(videoId: String, origin: String): String = """
    <!doctype html>
    <html><head>
      <meta name="viewport" content="width=device-width,initial-scale=1">
      <meta name="referrer" content="strict-origin-when-cross-origin">
      <style>html,body,#player {margin:0;width:100%;height:100%;background:#000}</style>
    </head><body>
      <div id="player"></div>
      <script>
        function report(status) { document.title = '${PLAYER_TITLE_PREFIX}' + status; }
        function onYouTubeIframeAPIReady() {
          new YT.Player('player', {
            videoId: '$videoId',
            playerVars: { playsinline: 1, origin: '$origin' },
            events: {
              onReady: function() { report('Ready'); },
              onStateChange: function(e) {
                if (e.data === YT.PlayerState.PLAYING) report('Playing');
                if (e.data === YT.PlayerState.PAUSED) report('Paused');
                if (e.data === YT.PlayerState.ENDED) report('Ended');
              },
              onError: function(e) {
                switch(e.data) {
                  case 101: case 150: report('Restricted'); break;
                  case 2: case 100: report('Unavailable'); break;
                  default: report('Failed');
                }
              }
            }
          });
        }
      </script>
      <script src="https://www.youtube.com/iframe_api" onerror="report('Failed')"></script>
    </body></html>
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeScreen(item: NewsItem, onPlaybackStarted: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx.findActivity()
    val latestPlaybackStarted by rememberUpdatedState(onPlaybackStarted)
    val origin = "https://${ctx.packageName}"
    val pageUrl = "$origin/"
    var online by remember(item.id) { mutableStateOf(Data.isOnline(ctx)) }
    var playback by remember(item.id) { mutableStateOf(Playback.Loading) }
    var fullscreen by remember(item.id) { mutableStateOf(false) }
    var chrome by remember(item.id) { mutableStateOf<FullscreenChrome?>(null) }
    var attempt by remember(item.id) { mutableStateOf(0) }
    var openFailed by remember(item.id) { mutableStateOf(false) }
    var thumb by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val validId = Data.isYoutubeId(item.id)

    LaunchedEffect(item.id, online, attempt) {
        thumb = withContext(Dispatchers.IO) {
            Data.thumbFile(ctx, item.id)?.let { BitmapFactory.decodeFile(it.absolutePath) }
        }
    }
    LaunchedEffect(item.id, online, attempt) {
        if (online && validId) {
            delay(15_000)
            if (playback == Playback.Loading) playback = Playback.Failed
        }
    }

    val playable = validId && online && playback !in
        setOf(Playback.Restricted, Playback.Unavailable, Playback.Failed)

    BackHandler(enabled = fullscreen) { chrome?.onHideCustomView() }

    Column(Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())) {
        if (playable) {
            key(item.id, attempt) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .aspectRatio(16f / 9f),
                    factory = { c ->
                        WebView(c).apply {
                            setBackgroundColor(AndroidColor.BLACK)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            webViewClient = object : WebViewClient() {
                                // The local wrapper is the only main document. YouTube
                                // can navigate its own frames, not replace that wrapper.
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ) = request.isForMainFrame && request.url.toString() != pageUrl

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError,
                                ) {
                                    if (request.isForMainFrame) playback = Playback.Failed
                                }

                                override fun onReceivedHttpError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    response: WebResourceResponse,
                                ) {
                                    if (request.isForMainFrame) playback = Playback.Failed
                                }
                            }
                            webChromeClient = FullscreenChrome(activity) { fullscreen = it }
                                .also { client ->
                                    client.onTitle = { title ->
                                        if (title.startsWith(PLAYER_TITLE_PREFIX)) {
                                            val state = Playback.entries.find {
                                                it.name == title.removePrefix(PLAYER_TITLE_PREFIX)
                                            }
                                            if (state != null) {
                                                playback = state
                                                if (state == Playback.Playing) latestPlaybackStarted()
                                            }
                                        }
                                    }
                                    chrome = client
                                }
                            loadDataWithBaseURL(
                                pageUrl, playerHtml(item.id, origin), "text/html", "UTF-8", pageUrl,
                            )
                        }
                    },
                    onRelease = { view ->
                        chrome?.let {
                            it.onTitle = {}
                            it.onHideCustomView()
                        }
                        view.stopLoading()
                        view.destroy()
                        chrome = null
                    },
                )
            }
        } else {
            // Offline (or the embed could not load): cover plus a plain notice.
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF141110)),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = thumb
                if (bmp != null) {
                    Image(
                        bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(0.dp)),
                    )
                    Box(Modifier.fillMaxSize().background(Color(0xB3141110)))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudOff,
                        null,
                        tint = Color(0xFFE9B44C),
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        when {
                            !online -> "You're offline"
                            !validId || playback == Playback.Unavailable -> "This video may be unavailable"
                            playback == Playback.Restricted -> "This edition plays on YouTube"
                            else -> "The in-app player couldn't load"
                        },
                        fontFamily = Ui,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2E7D3),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (online) {
                            "Use Watch on YouTube below to open the original edition."
                        } else {
                            "News editions need a connection. Songs, books and pledges are still available."
                        },
                        fontFamily = Ui,
                        fontSize = 11.sp,
                        color = Color(0xFFB3A491),
                        modifier = Modifier.padding(horizontal = 26.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    if (validId && (!online || playback == Playback.Failed)) Surface(
                        color = Color(0xFFE9B44C),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .padding(top = 2.dp),
                    ) {
                        Text(
                            "Retry",
                            fontFamily = Ui,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                                .clickable {
                                    online = Data.isOnline(ctx)
                                    playback = Playback.Loading
                                    attempt++
                                },
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                "HJ GLOBAL NEWS",
                fontFamily = Ui,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = Color(0xFFE9B44C),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                item.title,
                fontFamily = Lora,
                fontSize = 21.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF2E7D3),
            )
            Spacer(Modifier.height(12.dp))
            if (validId) Surface(
                color = Color(0xFFE9B44C),
                shape = RoundedCornerShape(50),
                modifier = Modifier.clickable {
                    openFailed = false
                    try {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(Data.youtubeUrl(item.id)))
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } catch (_: ActivityNotFoundException) {
                        openFailed = true
                    }
                },
            ) {
                Text(
                    "Watch on YouTube",
                    fontFamily = Ui,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
            if (openFailed) {
                Text(
                    "No app or browser is available to open YouTube.",
                    color = Color(0xFFF2E7D3),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                color = Color(0xFF141110),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        when {
                            !online -> "Offline"
                            !playable -> "In-app playback unavailable"
                            playback == Playback.Loading -> "Loading the player…"
                            playback == Playback.Playing -> "Playing in the app"
                            playback == Playback.Paused -> "Paused"
                            playback == Playback.Ended -> "Playback ended"
                            else -> "Ready to watch"
                        },
                        fontFamily = Ui,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE9B44C),
                    )
                    Text(
                        if (playable) {
                            "Tap play in the player, or use Watch on YouTube."
                        } else {
                            "When you're connected, Watch on YouTube opens the original edition."
                        },
                        fontFamily = Ui,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFB3A491),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }

}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Sends the embed's fullscreen request to a view layered over the activity. */
private class FullscreenChrome(
    private val activity: Activity?,
    private val onFullscreenChanged: (Boolean) -> Unit,
) : WebChromeClient() {
    var onTitle: (String) -> Unit = {}

    override fun onReceivedTitle(view: WebView?, title: String?) {
        title?.let(onTitle)
    }


    private var fullscreen: View? = null
    private var callback: CustomViewCallback? = null

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        val root = activity?.findViewById<ViewGroup>(android.R.id.content)
        if (root == null || fullscreen != null) {
            callback.onCustomViewHidden()
            return
        }
        fullscreen = view
        this.callback = callback
        (view.parent as? ViewGroup)?.removeView(view)
        view.setBackgroundColor(AndroidColor.BLACK)
        root.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        onFullscreenChanged(true)
    }

    override fun onHideCustomView() {
        val view = fullscreen ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        fullscreen = null
        callback?.onCustomViewHidden()
        callback = null
        onFullscreenChanged(false)
    }
}
