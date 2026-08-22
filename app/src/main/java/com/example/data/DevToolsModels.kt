package com.example.data

import androidx.compose.runtime.Immutable

enum class ConsoleLogLevel {
    LOG, WARN, ERROR, INFO
}

@Immutable
data class ConsoleLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: ConsoleLogLevel = ConsoleLogLevel.LOG,
    val message: String,
    val sourceUrl: String = "",
    val lineNumber: Int = 0
)

@Immutable
data class NetworkRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val method: String = "GET",
    val statusCode: Int = 200,
    val type: String = "fetch", // fetch, xhr, doc, script, stylesheet, image, font, other
    val durationMs: Long = 0,
    val requestHeaders: String = "{}",
    val requestBody: String = "",
    val responseHeaders: String = "{}",
    val responseBody: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class InspectedElement(
    val tagName: String = "",
    val id: String = "",
    val className: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val computedStyles: Map<String, String> = emptyMap(),
    val outerHtml: String = "",
    val innerHtml: String = "",
    val width: Float = 0f,
    val height: Float = 0f,
    val top: Float = 0f,
    val left: Float = 0f
)

@Immutable
data class DomNode(
    val nodeId: String,
    val tagName: String,
    val isTextNode: Boolean = false,
    val textContent: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val children: List<DomNode> = emptyList(),
    val isExpanded: Boolean = false
)

@Immutable
data class CookieItem(
    val name: String,
    val value: String,
    val domain: String = "",
    val path: String = "/"
)

@Immutable
data class StorageItem(
    val key: String,
    val value: String
)

@Immutable
data class BookmarkItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class HistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "https://example.com",
    val faviconUrl: String? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0
)

enum class UserAgentType(val label: String, val userAgentString: String) {
    MOBILE_CHROME(
        "Chrome Mobile (Android)",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UD1A.230803.022) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    ),
    DESKTOP_CHROME(
        "Chrome Desktop (Mac)",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    SAFARI_IPHONE(
        "Safari (iPhone 15)",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"
    ),
    DESKTOP_FIREFOX(
        "Firefox Desktop (Windows)",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"
    )
}

enum class DevToolsTab(val label: String) {
    ELEMENTS("Elements"),
    CONSOLE("Console"),
    NETWORK("Network"),
    SOURCES("Sources"),
    STORAGE("Storage"),
    DEVICES("Devices")
}
