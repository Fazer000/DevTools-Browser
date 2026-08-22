package com.example.ui.components

import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
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
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Setup WebView Settings
                settings.apply {
                    javaScriptEnabled = jsEnabled
                    domStorageEnabled = true
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
                        return false // Let WebView handle link clicks natively
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
