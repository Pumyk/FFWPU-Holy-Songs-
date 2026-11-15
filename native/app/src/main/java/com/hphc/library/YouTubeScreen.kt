package com.hphc.library

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/** YouTube embed URL - plays inline, and the fullscreen button works. */
private fun embedUrl(videoId: String): String =
    "https://www.youtube.com/embed/$videoId" +
        "?autoplay=1&playsinline=1&rel=0&modestbranding=1&fs=1"

/**
 * In-app YouTube player. A WebView running the official embed page: no API
 * key, no extra dependency, and the app never loses the viewer to YouTube.
 * The embed's own fullscreen button is routed to a real fullscreen view.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeScreen(item: NewsItem) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val holder = remember { arrayOfNulls<WebView>(1) }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
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
                    webViewClient = WebViewClient()
                    webChromeClient = FullscreenChrome(activity)
                    loadUrl(embedUrl(item.id))
                    holder[0] = this
                }
            },
        )

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
                        "Playing in the app",
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE9B44C),
                    )
                    Text(
                        "Use the player's fullscreen button for a bigger picture. " +
                            "The back arrow stops playback and returns to the news list.",
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
