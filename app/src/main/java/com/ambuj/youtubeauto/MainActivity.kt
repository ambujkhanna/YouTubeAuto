package com.ambuj.youtubeauto

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    private val allowedHosts = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be",
        "www.youtu.be",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com",
        "ytimg.com",
        "i.ytimg.com",
        "googlevideo.com",
        "googleusercontent.com",
        "ggpht.com",
        "google.com",
        "www.google.com",
        "accounts.google.com",
        "youtubei.googleapis.com"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.setBackgroundColor(Color.BLACK)

        webView.settings.apply {

            javaScriptEnabled = true
            domStorageEnabled = true

            mediaPlaybackRequiresUserGesture = false

            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)

            allowFileAccess = false
            allowContentAccess = false

            javaScriptCanOpenWindowsAutomatically = false

            // Privacy: location access remains disabled.
            setGeolocationEnabled(false)

            /*
             * Keep WebView scaling at its normal value.
             * This is important for testing the Android Auto
             * keyboard text-size issue.
             */
            textZoom = 100

            /*
             * Do not force desktop/overview scaling.
             * Let the responsive YouTube mobile website handle
             * the display size.
             */
            useWideViewPort = false
            loadWithOverviewMode = false

            /*
             * IMPORTANT:
             * Do not modify the default WebView User-Agent.
             *
             * The previous version appended:
             * "YouTubeAuto/0.1.0"
             *
             * Removing that gives YouTube and WebView their
             * normal browser/device detection behaviour.
             */
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        /*
         * Restrict navigation to approved YouTube/Google domains.
         */
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return !isAllowed(request.url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                return !isAllowed(Uri.parse(url))
            }
        }

        /*
         * Handle YouTube fullscreen video.
         */
        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowCustomView(
                view: View,
                callback: CustomViewCallback
            ) {

                if (fullscreenView != null) {
                    callback.onCustomViewHidden()
                    return
                }

                fullscreenView = view
                fullscreenCallback = callback

                findViewById<FrameLayout>(R.id.root).addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                webView.visibility = View.GONE

                hideSystemUi()
            }

            override fun onHideCustomView() {
                exitFullscreen()
            }
        }

        if (savedInstanceState == null) {

            /*
             * Use the mobile YouTube site for better responsive
             * sizing and touch targets on the car display.
             */
            webView.loadUrl("https://m.youtube.com/")

        } else {

            webView.restoreState(savedInstanceState)
        }
    }

    private fun isAllowed(uri: Uri): Boolean {

        val scheme = uri.scheme?.lowercase() ?: return false

        if (scheme != "https") {
            return false
        }

        val host = uri.host?.lowercase() ?: return false

        return allowedHosts.any {
            host == it || host.endsWith(".$it")
        }
    }

    private fun hideSystemUi() {

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    private fun exitFullscreen() {

        val view = fullscreenView ?: return

        (view.parent as? FrameLayout)?.removeView(view)

        fullscreenView = null

        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null

        webView.visibility = View.VISIBLE

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (fullscreenView != null) {

            exitFullscreen()

        } else if (webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {

        webView.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {

        if (fullscreenView != null) {
            exitFullscreen()
        }

        webView.stopLoading()
        webView.webChromeClient = null

        webView.destroy()

        super.onDestroy()
    }
}
