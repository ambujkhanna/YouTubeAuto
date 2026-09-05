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
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

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
        "google.com",
        "www.google.com",
        "accounts.google.com"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.setBackgroundColor(Color.BLACK)

        /*
         * Keep WebView configuration close to Android defaults.
         *
         * We deliberately do NOT modify the User-Agent or apply
         * global text scaling here, because those can cause unusual
         * rendering behaviour on the Android Auto display.
         */
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true

            mediaPlaybackRequiresUserGesture = false

            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)

            allowFileAccess = false
            allowContentAccess = false

            javaScriptCanOpenWindowsAutomatically = false

            // Keep normal WebView scaling.
            textZoom = 100

            // Let the mobile YouTube website control responsive layout.
            useWideViewPort = false
            loadWithOverviewMode = false
        }

        /*
         * Cookies are required for normal YouTube login/session behaviour.
         */
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        /*
         * Prevent forced dark mode from changing YouTube rendering.
         */
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(
                webView.settings,
                WebSettingsCompat.FORCE_DARK_OFF
            )
        }

        /*
         * Only allow HTTPS navigation to approved YouTube/Google domains.
         */
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return !isAllowed(request.url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                return !isAllowed(Uri.parse(url))
            }
        }

        /*
         * Handles YouTube fullscreen video.
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

                val root = findViewById<FrameLayout>(R.id.root)

                root.addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                webView.visibility = View.GONE

                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }

            override fun onHideCustomView() {
                exitFullscreen()
            }
        }

        /*
         * Use YouTube's mobile website.
         *
         * This should provide better responsive sizing and touch targets
         * on the Android Auto display.
         */
        webView.loadUrl("https://m.youtube.com/")
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

    private fun exitFullscreen() {

        val view = fullscreenView ?: return

        (view.parent as? FrameLayout)?.removeView(view)

        fullscreenView = null

        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null

        webView.visibility = View.VISIBLE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (fullscreenView != null) {

            exitFullscreen()

        } else if (webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {

        super.onNewIntent(intent)

        val url = intent?.data

        if (url != null && isAllowed(url)) {
            webView.loadUrl(url.toString())
        }
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
