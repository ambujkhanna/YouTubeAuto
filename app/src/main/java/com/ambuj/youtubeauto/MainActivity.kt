package com.ambuj.youtubeauto

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
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
import androidx.car.app.features.CarFeatures

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private var backgroundAudioWhileDrivingSupported = false
    private var mediaPausedByLifecycle = false

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

        backgroundAudioWhileDrivingSupported = try {
            CarFeatures.isFeatureEnabled(
                this,
                CarFeatures.FEATURE_BACKGROUND_AUDIO_WHILE_DRIVING
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query background-audio car capability", e)
            false
        }

        Log.i(
            TAG,
            "Background audio while driving supported: $backgroundAudioWhileDrivingSupported"
        )

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
            setGeolocationEnabled(false)

            // Keep WebView text/display scaling normal.
            textZoom = 100
            useWideViewPort = false
            loadWithOverviewMode = false
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

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

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (isAllowed(Uri.parse(url))) {
                    forcePlainTextSearchInputs(view)
                }
            }
        }

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
            webView.loadUrl("https://m.youtube.com/")
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    /**
     * YouTube's search field can be exposed to Android/Android Auto as a
     * search/URL-style input. Some car keyboards then show secondary
     * symbols prominently, making the letters hard to read.
     *
     * Change only YouTube search inputs to ordinary text input so the IME
     * can prefer its alphabetic layout. Android/Android Auto still owns
     * the actual keyboard, so the final layout remains device-dependent.
     */
    private fun forcePlainTextSearchInputs(view: WebView) {
        val script = """
            (function() {
                function fixSearchInputs() {
                    var inputs = document.querySelectorAll(
                        'input[type="search"], input[name="search_query"], ' +
                        'input[role="searchbox"], input[aria-label*="Search" i]'
                    );

                    inputs.forEach(function(input) {
                        if (!input || input.dataset.parkplayKeyboardFix === '1') return;

                        input.dataset.parkplayKeyboardFix = '1';

                        try {
                            input.setAttribute('type', 'text');
                        } catch (e) {}

                        input.setAttribute('inputmode', 'text');
                        input.setAttribute('autocapitalize', 'none');
                        input.setAttribute('autocomplete', 'off');
                        input.setAttribute('autocorrect', 'off');
                        input.setAttribute('spellcheck', 'false');
                    });
                }

                fixSearchInputs();

                if (!window.__parkplayKeyboardObserver) {
                    window.__parkplayKeyboardObserver =
                        new MutationObserver(fixSearchInputs);

                    window.__parkplayKeyboardObserver.observe(
                        document.documentElement,
                        {
                            childList: true,
                            subtree: true
                        }
                    );
                }
            })();
        """.trimIndent()

        view.evaluateJavascript(script, null)
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

    @Deprecated("Deprecated in Java")
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

    override fun onPause() {
        // Android Auto may pause/obscure parked apps when driving starts.
        // Explicitly pause HTML5 media before losing foreground control.
        mediaPausedByLifecycle = true
        Log.i(TAG, "Lifecycle pause: pausing HTML5 media")
        pauseHtml5Media()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        // Do not automatically resume playback here. If Android Auto has
        // returned control after a driving transition, the user can press
        // Play again while parked. This avoids accidentally starting media
        // during a restricted/driving state.
        if (mediaPausedByLifecycle) {
            Log.i(TAG, "Lifecycle resume: media remains paused until user starts playback")
            mediaPausedByLifecycle = false
            pauseHtml5Media()
        } else {
            Log.i(TAG, "Lifecycle resume: no car-induced media pause recorded")
        }
    }

    private fun pauseHtml5Media() {
        if (!::webView.isInitialized) return

        Log.d(TAG, "Requesting HTML5 video/audio pause")

        webView.evaluateJavascript(
            """
            (function() {
                try {
                    document.querySelectorAll('video, audio').forEach(function(media) {
                        try { media.pause(); } catch (e) {}
                    });
                } catch (e) {}
            })();
            """.trimIndent(),
            null
        )
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

    companion object {
        private const val TAG = "ParkPlay"
    }
}
