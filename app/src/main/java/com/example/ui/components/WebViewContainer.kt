package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.DevToolsBridge
import com.example.browser.InjectedScripts
import com.example.data.UserAgentType
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WebViewContainer(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val userAgentType by viewModel.userAgentType.collectAsState()
    val jsEnabled by viewModel.jsEnabled.collectAsState()
    val cacheEnabled by viewModel.cacheEnabled.collectAsState()
    val isInspectorActive by viewModel.isInspectorActive.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Handle incoming navigation actions from ViewModel
    LaunchedEffect(webViewRef) {
        val webView = webViewRef ?: return@LaunchedEffect
        viewModel.navigationAction.collectLatest { action ->
            when (action) {
                is BrowserViewModel.NavigationAction.LoadUrl -> {
                    webView.loadUrl(action.url)
                }
                BrowserViewModel.NavigationAction.GoBack -> {
                    if (webView.canGoBack()) webView.goBack()
                }
                BrowserViewModel.NavigationAction.GoForward -> {
                    if (webView.canGoForward()) webView.goForward()
                }
                BrowserViewModel.NavigationAction.Reload -> {
                    webView.reload()
                }
                BrowserViewModel.NavigationAction.StopLoading -> {
                    webView.stopLoading()
                }
                is BrowserViewModel.NavigationAction.EvaluateJs -> {
                    webView.evaluateJavascript(action.js, null)
                }
                is BrowserViewModel.NavigationAction.ToggleInspector -> {
                    webView.evaluateJavascript(InjectedScripts.getInspectorScript(action.enable), null)
                }
            }
        }
    }

    // Toggle inspector whenever state changes
    LaunchedEffect(isInspectorActive, webViewRef) {
        webViewRef?.evaluateJavascript(InjectedScripts.getInspectorScript(isInspectorActive), null)
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .testTag("webview_main"),
        factory = { context ->
            WebView(context).apply {
                val wv = this
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Setup CookieManager
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(wv, true)
                }

                // Setup WebView Settings
                settings.apply {
                    javaScriptEnabled = jsEnabled
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    allowContentAccess = true
                    allowFileAccess = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = userAgentType.userAgentString
                    cacheMode = if (cacheEnabled) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE
                }

                // Add Javascript Interface Bridge
                addJavascriptInterface(DevToolsBridge(viewModel), "AndroidDevTools")

                // Configure Custom WebViewClient
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { viewModel.onPageStarted(it) }
                        // Inject scripts at page start to intercept early network requests
                        view?.evaluateJavascript(InjectedScripts.CONSOLE_OVERRIDE_SCRIPT, null)
                        view?.evaluateJavascript(InjectedScripts.NETWORK_OVERRIDE_SCRIPT, null)
                    }

                    override fun onLoadResource(view: WebView?, url: String?) {
                        super.onLoadResource(view, url)
                        // Re-evaluate JS to catch dynamically loaded frames/scripts
                        view?.evaluateJavascript(InjectedScripts.NETWORK_OVERRIDE_SCRIPT, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (view != null && url != null) {
                            viewModel.onPageFinished(
                                url = url,
                                title = view.title,
                                canBack = view.canGoBack(),
                                canForward = view.canGoForward()
                            )

                            // Inject DevTools Scripts
                            view.evaluateJavascript(InjectedScripts.CONSOLE_OVERRIDE_SCRIPT, null)
                            view.evaluateJavascript(InjectedScripts.NETWORK_OVERRIDE_SCRIPT, null)
                            if (isInspectorActive) {
                                view.evaluateJavascript(InjectedScripts.getInspectorScript(true), null)
                            }

                            // Extract HTML source, DOM Tree, Storage
                            view.evaluateJavascript("document.documentElement.outerHTML") { htmlJson ->
                                if (htmlJson != null && htmlJson.length > 2) {
                                    val cleaned = htmlJson.trim('"').replace("\\u003C", "<").replace("\\\"", "\"").replace("\\n", "\n")
                                    viewModel.onPageSourceLoaded(cleaned)
                                }
                            }
                            view.evaluateJavascript(InjectedScripts.EXTRACT_DOM_TREE_JS, null)
                            view.evaluateJavascript(InjectedScripts.EXTRACT_STORAGE_JS, null)
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        return handleCustomIntentUrl(context, view, url)
                    }

                    @Suppress("DEPRECATION")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (url.isNullOrBlank()) return false
                        return handleCustomIntentUrl(context, view, url)
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                        val errorMsg = error?.toString() ?: "SSL Certificate Warning"
                        viewModel.onConsoleLogReceived("WARN", "SSL Certificate Warning: $errorMsg", view?.url ?: "")
                        handler?.proceed()
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val description = error?.description?.toString() ?: "Failed to load URL"
                            val code = error?.errorCode ?: 0
                            viewModel.onConsoleLogReceived("ERROR", "HTTP/Network Error [$code]: $description", request.url?.toString() ?: "")
                        }
                    }

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        if (request != null) {
                            val url = request.url.toString()
                            if (!url.startsWith("data:") && !url.startsWith("blob:") && !url.startsWith("about:") && !url.startsWith("javascript:")) {
                                val isMainFrame = request.isForMainFrame
                                val headersMap = request.requestHeaders ?: emptyMap()
                                val method = request.method ?: "GET"

                                val acceptHeader = headersMap["Accept"] ?: headersMap["accept"] ?: ""
                                val xReqWith = headersMap["X-Requested-With"] ?: headersMap["x-requested-with"] ?: ""
                                val secFetchMode = headersMap["Sec-Fetch-Mode"] ?: headersMap["sec-fetch-mode"] ?: ""
                                val dest = headersMap["Sec-Fetch-Dest"] ?: headersMap["sec-fetch-dest"] ?: ""

                                val lowerUrl = url.lowercase()

                                val isFetchOrXhr = xReqWith.equals("XMLHttpRequest", ignoreCase = true) ||
                                        dest.equals("empty", ignoreCase = true) ||
                                        dest.equals("serviceworker", ignoreCase = true) ||
                                        secFetchMode.contains("cors", ignoreCase = true) ||
                                        acceptHeader.contains("json", ignoreCase = true) ||
                                        lowerUrl.contains("/api/") ||
                                        lowerUrl.contains("graphql") ||
                                        lowerUrl.contains("/v1/") || lowerUrl.contains("/v2/") || lowerUrl.contains("/v3/")

                                val type = when {
                                    isMainFrame -> "doc"
                                    isFetchOrXhr -> "fetch"
                                    lowerUrl.contains(".js") || lowerUrl.contains(".js?") -> "js"
                                    lowerUrl.contains(".css") || lowerUrl.contains(".css?") -> "css"
                                    lowerUrl.contains(".png") || lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || lowerUrl.contains(".gif") || lowerUrl.contains(".svg") || lowerUrl.contains(".webp") || lowerUrl.contains(".ico") || lowerUrl.contains(".avif") -> "img"
                                    lowerUrl.contains(".mp4") || lowerUrl.contains(".webm") || lowerUrl.contains(".m3u8") || lowerUrl.contains(".mp3") || lowerUrl.contains(".wav") || lowerUrl.contains(".ts") || lowerUrl.contains(".ogg") || lowerUrl.contains(".aac") -> "media"
                                    lowerUrl.contains(".woff") || lowerUrl.contains(".ttf") || lowerUrl.contains(".eot") || lowerUrl.contains(".otf") -> "font"
                                    dest.equals("iframe", ignoreCase = true) || dest.equals("frame", ignoreCase = true) -> "doc"
                                    else -> "other"
                                }

                                val json = org.json.JSONObject().apply {
                                    put("url", url)
                                    put("method", method)
                                    put("statusCode", 200)
                                    put("statusText", "OK")
                                    put("type", type)
                                    put("durationMs", 0)
                                    put("sizeBytes", 0)
                                    put("initiator", if (isMainFrame) "MainFrame" else if (isFetchOrXhr) "Fetch/XHR Engine" else "WebView Engine")
                                    put("requestHeaders", org.json.JSONObject(headersMap))
                                    put("responseHeaders", org.json.JSONObject())
                                    put("responseBody", if (isFetchOrXhr) "[Fetch/XHR Pending/Native]" else "[Native WebView Resource]")
                                }.toString()

                                viewModel.onNetworkRequestJsonReceived(json)
                            }
                        }
                        // Always return null to let WebView execute network requests natively with full POST bodies, cookies, and OAuth params
                        return null
                    }
                }

                // Configure Custom WebChromeClient
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        viewModel.onProgressChanged(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { viewModel.onTitleChanged(it) }
                    }

                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                        super.onReceivedIcon(view, icon)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage != null) {
                            viewModel.onConsoleLogReceived(
                                level = consoleMessage.messageLevel().name,
                                message = consoleMessage.message(),
                                sourceUrl = "${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                            )
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                loadUrl(currentUrl)
                webViewRef = this
            }
        },
        update = { webView ->
            webViewRef = webView
            if (webView.settings.userAgentString != userAgentType.userAgentString) {
                webView.settings.userAgentString = userAgentType.userAgentString
            }
            if (webView.settings.javaScriptEnabled != jsEnabled) {
                webView.settings.javaScriptEnabled = jsEnabled
            }
            webView.settings.cacheMode = if (cacheEnabled) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE
        }
    )
}

private fun handleCustomIntentUrl(context: Context, view: WebView?, url: String): Boolean {
    val lower = url.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://") ||
        lower.startsWith("file://") || lower.startsWith("data:") ||
        lower.startsWith("about:") || lower.startsWith("javascript:")) {
        return false // Let WebView handle web pages natively
    }

    try {
        if (url.startsWith("intent://") || url.startsWith("intent:")) {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.component = null
                intent.selector = null

                val packageManager = context.packageManager
                val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                if (resolveInfo != null) {
                    context.startActivity(intent)
                    return true
                }

                // Fallback URL if target application is not installed
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrBlank()) {
                    view?.loadUrl(fallbackUrl)
                    return true
                }

                // Market / Play Store fallback if package is present
                val packageName = intent.`package`
                if (!packageName.isNullOrBlank()) {
                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    if (packageManager.resolveActivity(marketIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        context.startActivity(marketIntent)
                        return true
                    }
                }
            }
        } else {
            // Custom protocols: mailto:, tel:, sms:, geo:, whatsapp:, tg:, market:, etc.
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY) != null) {
                context.startActivity(intent)
                return true
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    Toast.makeText(context, "Приложение не найдено для ссылки: ${url.take(60)}", Toast.LENGTH_SHORT).show()
    return true
}
