package com.magopato.voltanalytics

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    companion object {
        private const val HOME_URL = "https://magopato.github.io/VoltAnalytics/"
    }

    private lateinit var webView: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var loadingView: View
    private lateinit var errorView: View
    private lateinit var errorText: TextView

    /** Márgenes seguros en px CSS, calculados con los WindowInsets / densidad. */
    private var safeTopCss = 0f
    private var safeBottomCss = 0f

    /** Lo actualiza el JavaScript inyectado: true si el contenido está arriba del todo. */
    @Volatile private var pageAtTop = true
    private var mainFrameFailed = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Borde a borde: la web pasa por detrás de las barras del sistema, con iconos claros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        swipe = findViewById(R.id.swipe)
        loadingView = findViewById(R.id.loading)
        errorView = findViewById(R.id.error)
        errorText = findViewById(R.id.error_text)
        findViewById<Button>(R.id.retry).setOnClickListener { retry() }

        // Insets de las barras del sistema -> px CSS para el CSS inyectado.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val density = resources.displayMetrics.density
            safeTopCss = bars.top / density
            safeBottomCss = bars.bottom / density
            swipe.setProgressViewOffset(false, bars.top, bars.top + (64 * density).toInt())
            loadingView.setPadding(0, bars.top, 0, bars.bottom)
            val p = (32 * density).toInt()
            errorView.setPadding(p, bars.top + p, p, bars.bottom + p)
            injectSafeAreaCss()
            insets
        }

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            javaScriptCanOpenWindowsAutomatically = true
            // Sin ventanas múltiples: window.open y target=_blank se abren en este mismo WebView.
            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.setBackgroundColor(Color.parseColor("#0b0f10"))
        webView.addJavascriptInterface(ScrollBridge(), "VoltAndroid")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = VoltWebViewClient()

        // Tirar para recargar solo cuando la página está arriba del todo.
        swipe.setColorSchemeColors(Color.parseColor("#2b8a83"))
        swipe.setProgressBackgroundColorSchemeColor(Color.parseColor("#1a2022"))
        swipe.setOnChildScrollUpCallback { _, _ ->
            webView.scrollY > 0 || webView.canScrollVertically(-1) || !pageAtTop
        }
        swipe.setOnRefreshListener { webView.reload() }

        // Botón "atrás" del teléfono: navegar hacia atrás dentro de la web.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            loadingView.visibility = View.GONE
        } else {
            webView.loadUrl(HOME_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private fun retry() {
        mainFrameFailed = false
        errorView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
        val current = webView.url
        if (current.isNullOrEmpty() || current == "about:blank") webView.loadUrl(HOME_URL)
        else webView.reload()
    }

    /** CSS que estira la barra superior de la web hasta arriba, con fondo translúcido desenfocado. */
    private fun injectSafeAreaCss() {
        if (!::webView.isInitialized) return
        val top = "${safeTopCss}px"
        val bottom = "${safeBottomCss}px"
        val css = ".topbar{height:calc(var(--header-h,52px) + $top)!important;" +
            "padding-top:$top!important;background:rgba(10,10,10,.55)!important;" +
            "-webkit-backdrop-filter:saturate(180%) blur(24px);backdrop-filter:saturate(180%) blur(24px)}" +
            ".sidebar{padding-top:$top!important;padding-bottom:$bottom!important}"
        val js = "(function(){" +
            "var id='volt-android-safe-area';" +
            "var s=document.getElementById(id);" +
            "if(!s){s=document.createElement('style');s.id=id;(document.head||document.documentElement).appendChild(s);}" +
            "s.textContent=" + JSONObject.quote(css) + ";" +
            "if(!window.__voltScrollHook){window.__voltScrollHook=true;" +
            "window.addEventListener('scroll',function(e){var t=e.target,y;" +
            "if(t===document||t===document.documentElement||t===document.body){y=window.scrollY||0;}" +
            "else{y=t.scrollTop||0;}" +
            "try{VoltAndroid.setAtTop(y<=0);}catch(_){}},true);}" +
            "})();"
        webView.evaluateJavascript(js, null)
    }

    private inner class ScrollBridge {
        @JavascriptInterface
        fun setAtTop(atTop: Boolean) {
            pageAtTop = atTop
        }
    }

    private inner class VoltWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            return when (uri.scheme?.lowercase()) {
                // http/https (incluida la redirección a *.trycloudflare.com) se queda en el WebView.
                "http", "https", "about", "data", "blob", "javascript" -> false
                // Otros esquemas (tel:, mailto:, intent:...) los abre el sistema si puede.
                else -> {
                    try {
                        val intent = if (uri.scheme.equals("intent", true))
                            Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                        else Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    } catch (_: Exception) {
                    }
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            pageAtTop = true
            mainFrameFailed = false
        }

        override fun onPageFinished(view: WebView, url: String?) {
            swipe.isRefreshing = false
            injectSafeAreaCss()
            if (!mainFrameFailed) {
                errorView.visibility = View.GONE
                loadingView.visibility = View.GONE
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            // Solo cuentan los errores del documento principal; los de subrecursos se ignoran.
            if (!request.isForMainFrame) return
            mainFrameFailed = true
            swipe.isRefreshing = false
            loadingView.visibility = View.GONE
            errorText.text = "${error.description} (código ${error.errorCode})"
            errorView.visibility = View.VISIBLE
        }
    }
}
