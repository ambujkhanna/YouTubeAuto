package com.ambuj.youtubeauto

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class CastViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlInput: EditText

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cast_viewer)

        urlInput = findViewById(R.id.castUrl)
        webView = findViewById(R.id.castWebView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        findViewById<Button>(R.id.openCast).setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                webView.loadUrl(url)
            }
        }

        // Keep a ready-to-use local default. The field remains editable when
        // the phone gets a different IP address.
        val initialUrl = intent.getStringExtra(EXTRA_URL).orEmpty().ifEmpty { DEFAULT_URL }
        urlInput.setText(initialUrl)
        webView.loadUrl(initialUrl)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "cast_url"
        const val DEFAULT_URL = "http://192.168.1.7:8080"
    }
}
