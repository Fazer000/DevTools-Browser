package com.example.browser

import android.webkit.JavascriptInterface

interface DevToolsBridgeListener {
    fun onConsoleLogReceived(level: String, message: String, sourceUrl: String)
    fun onNetworkRequestReceived(
        url: String, method: String, statusCode: Int, type: String, durationMs: Long,
        reqHeaders: String, reqBody: String, resHeaders: String, resBody: String
    )
    fun onNetworkRequestJsonReceived(jsonString: String)
    fun onWebSocketFrameReceived(wsUrl: String, direction: String, payload: String)
    fun onElementInspectedReceived(jsonString: String)
    fun onDomTreeExtractedReceived(jsonString: String)
    fun onStorageExtractedReceived(localStorageJson: String, sessionStorageJson: String, cookieString: String)
}

class DevToolsBridge(private val listener: DevToolsBridgeListener) {

    @JavascriptInterface
    fun onConsoleLog(level: String, message: String, sourceUrl: String) {
        listener.onConsoleLogReceived(level, message, sourceUrl)
    }

    @JavascriptInterface
    fun onNetworkRequest(
        url: String, method: String, statusCode: Int, type: String, durationMs: Long,
        reqHeaders: String, reqBody: String, resHeaders: String, resBody: String
    ) {
        listener.onNetworkRequestReceived(
            url, method, statusCode, type, durationMs, reqHeaders, reqBody, resHeaders, resBody
        )
    }

    @JavascriptInterface
    fun onNetworkRequestJson(jsonString: String) {
        listener.onNetworkRequestJsonReceived(jsonString)
    }

    @JavascriptInterface
    fun onWebSocketFrame(wsUrl: String, direction: String, payload: String) {
        listener.onWebSocketFrameReceived(wsUrl, direction, payload)
    }

    @JavascriptInterface
    fun onElementInspected(jsonString: String) {
        listener.onElementInspectedReceived(jsonString)
    }

    @JavascriptInterface
    fun onDomTreeExtracted(jsonString: String) {
        listener.onDomTreeExtractedReceived(jsonString)
    }

    @JavascriptInterface
    fun onStorageExtracted(localStorageJson: String, sessionStorageJson: String, cookieString: String) {
        listener.onStorageExtractedReceived(localStorageJson, sessionStorageJson, cookieString)
    }
}
