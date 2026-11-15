package com.hphc.library

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.withContext

/** YouTube embed URL - plays inline, and the fullscreen button works. */
private fun embedUrl(videoId: String): String =
    "https://www.youtube.com/embed/$videoId" +
        "?autoplay=1&playsinline=1&rel=0&modestbranding=1&fs=1"

/**
 * In-app YouTube player. A WebView running the official embed page: no API
 * key, no extra dependency, and the app never loses the viewer to YouTube.
 * The embed's own fullscreen button is routed to a real fullscreen view.
 *
 * If the phone is offline, the WebView is never asked to load: the screen
 * shows the cached cover and a clear offline notice instead of a network
 * error page.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeScreen(item: NewsItem) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val holder = remember { arrayOfNulls<WebView>(1) }

    var online by remember { mutableStateOf(Data.isOnline(ctx)) }
    var loadFailed by remember { mutableStateOf(false) }
    var thumb by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(item.id) {
        val file = withContext(Dispatchers.IO) { Data.thumbFile(ctx, item.id) }
        if (file != null) {
            thumb = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.absolutePath) }
        }
    }

    val playable = online && !loadFailed

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        if (playable) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                factory = { c ->
                    WebView(c).apply {
                        setBackgroundColor(AndroidColor.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) loadFailed = true
                            }
                        }
                        webChromeClient = FullscreenChrome(activity)
                        loadUrl(embedUrl(item.id))
                        holder[0] = this
                    }
                },
            )
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
                        if (online) "This edition can't load right now" else "You're offline",
                        fontFamily = Inter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2E7D3),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (online) {
                            "Check your connection and reopen this edition."
                        } else {
                            "Every song, book and pledge still works offline. " +
                                "News editions need a connection."
                        },
                        fontFamily = Inter,
                        fontSize = 11.sp,
                        color = Color(0xFFB3A491),
                        modifier = Modifier.padding(horizontal = 26.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFE9B44C),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .padding(top = 2.dp),
                    ) {
                        Text(
                            "Retry",
                            fontFamily = Inter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                                .clickable {
                                    online = Data.isOnline(ctx)
                                    loadFailed = false
                                },
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                "LATEST EDITION",
                fontFamily = Inter,
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
            Surface(
                color = Color(0xFF141110),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        if (playable) "Playing in the app" else "Saved for later",
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE9B44C),
                    )
                    Text(
                        if (playable) {
                            "Use the player's fullscreen button for a bigger picture. " +
                                "The back arrow stops playback and returns to the news list."
                        } else {
                            "The cover for this edition stays on the phone. Open it again " +
                                "when you have a connection and it plays in the app."
                        },
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFB3A491),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            holder[0]?.apply {
                loadUrl("about:blank")
                destroy()
            }
            holder[0] = null
        }
    }
}

/** Sends the embed's fullscreen request to a view layered over the activity. */
private class FullscreenChrome(private val activity: Activity?) : WebChromeClient() {

    private var fullscreen: View? = null
    private var callback: CustomViewCallback? = null

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        val root = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (fullscreen != null) return
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
    }

    override fun onHideCustomView() {
        val view = fullscreen ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        fullscreen = null
        callback?.onCustomViewHidden()
        callback = null
    }
}
