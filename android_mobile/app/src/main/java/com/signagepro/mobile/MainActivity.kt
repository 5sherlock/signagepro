package com.signagepro.mobile

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        // ─── 접속할 서버 주소 ───────────────────────────────────────────────────
        private const val SERVER_URL = "https://jeju-osulloc.tailafea2d.ts.net/mobile"
        // ──────────────────────────────────────────────────────────────────────
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: LinearLayout

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── 루트 레이아웃 ──────────────────────────────────────────────────────
        val root = FrameLayout(this)
        root.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        root.setBackgroundColor(Color.parseColor("#0D1117"))

        // ── WebView ────────────────────────────────────────────────────────────
        webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

        webView.settings.apply {
            javaScriptEnabled        = true
            domStorageEnabled        = true
            allowFileAccess          = true
            useWideViewPort          = true
            loadWithOverviewMode     = true
            setSupportZoom           (false)
            displayZoomControls      = false
            cacheMode                = WebSettings.LOAD_DEFAULT
            @Suppress("DEPRECATION")
            mixedContentMode         = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // localStorage / Socket.IO 지원에 필요
            databaseEnabled          = true
        }

        // ── 진행 바 ────────────────────────────────────────────────────────────
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, 6)
        progressBar.max          = 100
        progressBar.progressTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#4F8EF7"))

        // ── 오프라인 오류 화면 ─────────────────────────────────────────────────
        errorView = LinearLayout(this)
        errorView.orientation = LinearLayout.VERTICAL
        errorView.gravity     = android.view.Gravity.CENTER
        errorView.visibility  = View.GONE
        errorView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        errorView.setBackgroundColor(Color.parseColor("#0D1117"))

        val errIcon = TextView(this)
        errIcon.text     = "📡"
        errIcon.textSize = 56f
        errIcon.gravity  = android.view.Gravity.CENTER

        val errTitle = TextView(this)
        errTitle.text      = "서버에 연결할 수 없습니다"
        errTitle.textSize  = 18f
        errTitle.setTextColor(Color.WHITE)
        errTitle.gravity   = android.view.Gravity.CENTER
        errTitle.setPadding(0, 24, 0, 8)

        val errSub = TextView(this)
        errSub.text      = SERVER_URL
        errSub.textSize  = 13f
        errSub.setTextColor(Color.parseColor("#8B949E"))
        errSub.gravity   = android.view.Gravity.CENTER

        val retryBtn = android.widget.Button(this)
        retryBtn.text = "다시 시도"
        retryBtn.setTextColor(Color.WHITE)
        retryBtn.setBackgroundColor(Color.parseColor("#1F6FEB"))
        retryBtn.setPadding(64, 24, 64, 24)
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnParams.topMargin = 40
        retryBtn.layoutParams = btnParams
        retryBtn.setOnClickListener { loadPage() }

        errorView.addView(errIcon)
        errorView.addView(errTitle)
        errorView.addView(errSub)
        errorView.addView(retryBtn)

        // ── WebViewClient ──────────────────────────────────────────────────────
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = View.GONE
                errorView.visibility   = View.GONE
                webView.visibility     = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    webView.visibility   = View.INVISIBLE
                    errorView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                }
            }
        }

        // ── WebChromeClient (진행 바) ───────────────────────────────────────────
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        // ── 레이아웃 조립 ──────────────────────────────────────────────────────
        root.addView(webView)
        root.addView(errorView)
        root.addView(progressBar)
        setContentView(root)

        loadPage()
    }

    private fun loadPage() {
        errorView.visibility = View.GONE
        webView.visibility   = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        webView.loadUrl(SERVER_URL)
    }

    /** 뒤로가기 버튼 → 웹 히스토리 내 이동 */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
