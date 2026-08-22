package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.DevToolsBridgeListener
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class BrowserViewModel(application: Application) : AndroidViewModel(application), DevToolsBridgeListener {

    private val repository = UserPreferencesRepository(application)

    // Primary State
    private val _currentUrl = MutableStateFlow("https://example.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("Example Domain")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _faviconUrl = MutableStateFlow<String?>(null)
    val faviconUrl: StateFlow<String?> = _faviconUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadProgress = MutableStateFlow(0)
    val loadProgress: StateFlow<Int> = _loadProgress.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    // DevTools UI State
    private val _isDevToolsVisible = MutableStateFlow(true)
    val isDevToolsVisible: StateFlow<Boolean> = _isDevToolsVisible.asStateFlow()

    private val _devToolsHeightFraction = MutableStateFlow(0.45f)
    val devToolsHeightFraction: StateFlow<Float> = _devToolsHeightFraction.asStateFlow()

    private val _activeDevToolsTab = MutableStateFlow(DevToolsTab.ELEMENTS)
    val activeDevToolsTab: StateFlow<DevToolsTab> = _activeDevToolsTab.asStateFlow()

    private val _isInspectorActive = MutableStateFlow(false)
    val isInspectorActive: StateFlow<Boolean> = _isInspectorActive.asStateFlow()

    private val _inspectedElement = MutableStateFlow<InspectedElement?>(null)
    val inspectedElement: StateFlow<InspectedElement?> = _inspectedElement.asStateFlow()

    // Tabs
    private val _tabs = MutableStateFlow(listOf(BrowserTab(url = "https://example.com", title = "Example Domain")))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    // Console Logs
    private val _consoleLogs = MutableStateFlow<List<ConsoleLog>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLog>> = _consoleLogs.asStateFlow()

    private val _consoleFilterLevel = MutableStateFlow<ConsoleLogLevel?>(null)
    val consoleFilterLevel: StateFlow<ConsoleLogLevel?> = _consoleFilterLevel.asStateFlow()

    private val _consoleSearchQuery = MutableStateFlow("")
    val consoleSearchQuery: StateFlow<String> = _consoleSearchQuery.asStateFlow()

    // Network Requests
    private val _networkRequests = MutableStateFlow<List<NetworkRequest>>(emptyList())
    val networkRequests: StateFlow<List<NetworkRequest>> = _networkRequests.asStateFlow()

    private val _selectedNetworkRequest = MutableStateFlow<NetworkRequest?>(null)
    val selectedNetworkRequest: StateFlow<NetworkRequest?> = _selectedNetworkRequest.asStateFlow()

    // Persistent Network Search & Filter Preferences
    val networkSearchQuery: StateFlow<String> = repository.netSearchQueryFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val networkExcludeQuery: StateFlow<String> = repository.netExcludeQueryFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val networkFilterType: StateFlow<String?> = repository.netFilterTypeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val networkSearchBody: StateFlow<Boolean> = repository.netSearchBodyFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val networkRegexMode: StateFlow<Boolean> = repository.netRegexModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // DOM Tree
    private val _domTree = MutableStateFlow<DomNode?>(null)
    val domTree: StateFlow<DomNode?> = _domTree.asStateFlow()

    // Storage
    private val _cookies = MutableStateFlow<List<CookieItem>>(emptyList())
    val cookies: StateFlow<List<CookieItem>> = _cookies.asStateFlow()

    private val _localStorage = MutableStateFlow<List<StorageItem>>(emptyList())
    val localStorage: StateFlow<List<StorageItem>> = _localStorage.asStateFlow()

    private val _sessionStorage = MutableStateFlow<List<StorageItem>>(emptyList())
    val sessionStorage: StateFlow<List<StorageItem>> = _sessionStorage.asStateFlow()

    // Page Source
    private val _pageSource = MutableStateFlow<String>("")
    val pageSource: StateFlow<String> = _pageSource.asStateFlow()

    // Preferences
    val userAgentType: StateFlow<UserAgentType> = repository.userAgentTypeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, UserAgentType.MOBILE_CHROME)

    val jsEnabled: StateFlow<Boolean> = repository.jsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val cacheEnabled: StateFlow<Boolean> = repository.cacheEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val bookmarks: StateFlow<List<BookmarkItem>> = repository.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dialogs / Drawers
    private val _isBookmarksDrawerOpen = MutableStateFlow(false)
    val isBookmarksDrawerOpen: StateFlow<Boolean> = _isBookmarksDrawerOpen.asStateFlow()

    private val _isTabsDialogOpen = MutableStateFlow(false)
    val isTabsDialogOpen: StateFlow<Boolean> = _isTabsDialogOpen.asStateFlow()

    // Event triggers for WebView
    private val _navigationAction = MutableSharedFlow<NavigationAction>()
    val navigationAction: SharedFlow<NavigationAction> = _navigationAction.asSharedFlow()

    sealed class NavigationAction {
        data class LoadUrl(val url: String) : NavigationAction()
        object GoBack : NavigationAction()
        object GoForward : NavigationAction()
        object Reload : NavigationAction()
        object StopLoading : NavigationAction()
        data class EvaluateJs(val js: String) : NavigationAction()
        data class ToggleInspector(val enable: Boolean) : NavigationAction()
    }

    init {
        viewModelScope.launch {
            repository.lastUrlFlow.collect { url ->
                if (_currentUrl.value == "https://example.com" && url.isNotBlank()) {
                    _currentUrl.value = url
                }
            }
        }
        viewModelScope.launch {
            repository.devToolsHeightFlow.collect { height ->
                _devToolsHeightFraction.value = height
            }
        }
        viewModelScope.launch {
            repository.devToolsVisibleFlow.collect { visible ->
                _isDevToolsVisible.value = visible
            }
        }
    }

    // Actions
    fun navigateToUrl(input: String) {
        var formatted = input.trim()
        if (formatted.isBlank()) return

        if (!formatted.startsWith("http://") && !formatted.startsWith("https://") && !formatted.startsWith("file://") && !formatted.startsWith("data:")) {
            if (formatted.contains(".") && !formatted.contains(" ")) {
                formatted = "https://$formatted"
            } else {
                formatted = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(formatted, "UTF-8")
            }
        }

        _currentUrl.value = formatted
        _inspectedElement.value = null
        _isInspectorActive.value = false

        viewModelScope.launch {
            _navigationAction.emit(NavigationAction.LoadUrl(formatted))
            repository.saveLastUrl(formatted)
        }
    }

    fun goBack() {
        viewModelScope.launch { _navigationAction.emit(NavigationAction.GoBack) }
    }

    fun goForward() {
        viewModelScope.launch { _navigationAction.emit(NavigationAction.GoForward) }
    }

    fun reload() {
        _networkRequests.value = emptyList()
        _consoleLogs.value = emptyList()
        viewModelScope.launch { _navigationAction.emit(NavigationAction.Reload) }
    }

    fun stopLoading() {
        viewModelScope.launch { _navigationAction.emit(NavigationAction.StopLoading) }
    }

    fun toggleInspector() {
        val newState = !_isInspectorActive.value
        _isInspectorActive.value = newState
        if (newState) {
            _isDevToolsVisible.value = true
            _activeDevToolsTab.value = DevToolsTab.ELEMENTS
        }
        viewModelScope.launch {
            _navigationAction.emit(NavigationAction.ToggleInspector(newState))
        }
    }

    fun toggleDevToolsVisibility() {
        val newVisible = !_isDevToolsVisible.value
        _isDevToolsVisible.value = newVisible
        viewModelScope.launch { repository.saveDevToolsVisible(newVisible) }
    }

    fun updateDevToolsHeight(fraction: Float) {
        val coerced = fraction.coerceIn(0.15f, 0.85f)
        _devToolsHeightFraction.value = coerced
        viewModelScope.launch { repository.saveDevToolsHeight(coerced) }
    }

    fun setDevToolsTab(tab: DevToolsTab) {
        _activeDevToolsTab.value = tab
    }

    fun executeJsInConsole(code: String) {
        if (code.isBlank()) return
        addConsoleLog(ConsoleLog(level = ConsoleLogLevel.INFO, message = "> $code"))
        val script = "(function() { try { var res = eval(${JSONObject.quote(code)}); return JSON.stringify(res); } catch(e) { return 'Error: ' + e.message; } })()"
        viewModelScope.launch {
            _navigationAction.emit(NavigationAction.EvaluateJs(script))
        }
    }

    fun clearConsole() {
        _consoleLogs.value = emptyList()
    }

    fun setConsoleFilterLevel(level: ConsoleLogLevel?) {
        _consoleFilterLevel.value = level
    }

    fun setConsoleSearchQuery(query: String) {
        _consoleSearchQuery.value = query
    }

    fun clearNetworkLogs() {
        _networkRequests.value = emptyList()
        _selectedNetworkRequest.value = null
    }

    fun selectNetworkRequest(request: NetworkRequest?) {
        _selectedNetworkRequest.value = request
    }

    fun setNetworkSearchQuery(query: String) {
        viewModelScope.launch {
            repository.saveNetworkSearchQuery(query)
        }
    }

    fun setNetworkExcludeQuery(query: String) {
        viewModelScope.launch {
            repository.saveNetworkExcludeQuery(query)
        }
    }

    fun setNetworkFilterType(type: String?) {
        viewModelScope.launch {
            repository.saveNetworkFilterType(type)
        }
    }

    fun setNetworkSearchBody(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveNetworkSearchBody(enabled)
        }
    }

    fun setNetworkRegexMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveNetworkRegexMode(enabled)
        }
    }

    fun addExcludeWord(word: String) {
        val cleanWord = word.trim().trimStart('-', '!', ',')
        if (cleanWord.isBlank()) return
        val currentWords = networkExcludeQuery.value
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        if (!currentWords.contains(cleanWord)) {
            val updated = (currentWords + cleanWord).joinToString(", ")
            setNetworkExcludeQuery(updated)
        }
    }

    fun removeExcludeWord(word: String) {
        val currentWords = networkExcludeQuery.value
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()
        currentWords.removeAll { it.equals(word.trim(), ignoreCase = true) }
        setNetworkExcludeQuery(currentWords.joinToString(", "))
    }

    fun clearNetworkFilters() {
        viewModelScope.launch {
            repository.saveNetworkSearchQuery("")
            repository.saveNetworkExcludeQuery("")
            repository.saveNetworkFilterType(null)
            repository.saveNetworkSearchBody(false)
            repository.saveNetworkRegexMode(false)
        }
    }

    fun setUserAgent(userAgentType: UserAgentType) {
        viewModelScope.launch {
            repository.saveUserAgentType(userAgentType)
            reload()
        }
    }

    fun setJsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveJsEnabled(enabled)
            reload()
        }
    }

    fun setCacheEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveCacheEnabled(enabled)
            reload()
        }
    }

    fun toggleBookmarkCurrentPage() {
        val url = _currentUrl.value
        val title = _pageTitle.value
        val currentBookmarks = bookmarks.value
        viewModelScope.launch {
            if (currentBookmarks.any { it.url == url }) {
                repository.removeBookmark(url)
            } else {
                repository.addBookmark(title, url)
            }
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch { repository.removeBookmark(url) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun setBookmarksDrawerOpen(open: Boolean) {
        _isBookmarksDrawerOpen.value = open
    }

    fun setTabsDialogOpen(open: Boolean) {
        _isTabsDialogOpen.value = open
    }

    fun addNewTab(url: String = "https://example.com") {
        val newTab = BrowserTab(url = url, title = "New Tab")
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        navigateToUrl(url)
    }

    fun closeTab(tabId: String) {
        if (_tabs.value.size <= 1) return
        val updated = _tabs.value.filter { it.id != tabId }
        _tabs.value = updated
        if (_activeTabId.value == tabId) {
            val nextTab = updated.last()
            _activeTabId.value = nextTab.id
            navigateToUrl(nextTab.url)
        }
    }

    fun switchTab(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        _activeTabId.value = tabId
        navigateToUrl(tab.url)
    }

    // Callbacks from WebView Navigation & WebChromeClient
    fun onPageStarted(url: String) {
        _currentUrl.value = url
        _isLoading.value = true
        _loadProgress.value = 10
        viewModelScope.launch { repository.saveLastUrl(url) }
    }

    fun onPageFinished(url: String, title: String?, canBack: Boolean, canForward: Boolean) {
        _currentUrl.value = url
        _isLoading.value = false
        _loadProgress.value = 100
        _canGoBack.value = canBack
        _canGoForward.value = canForward

        if (!title.isNullOrBlank()) {
            _pageTitle.value = title
            viewModelScope.launch { repository.addHistory(title, url) }
        }

        // Update current active tab title
        val currentTabId = _activeTabId.value
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == currentTabId) tab.copy(title = title ?: url, url = url) else tab
        }
    }

    fun onProgressChanged(progress: Int) {
        _loadProgress.value = progress
        _isLoading.value = progress < 100
    }

    fun onTitleChanged(title: String) {
        _pageTitle.value = title
    }

    fun onFaviconChanged(faviconUrl: String?) {
        _faviconUrl.value = faviconUrl
    }

    fun onPageSourceLoaded(source: String) {
        _pageSource.value = source
    }

    // DevToolsBridge Implementation
    override fun onConsoleLogReceived(level: String, message: String, sourceUrl: String) {
        val logLevel = try { ConsoleLogLevel.valueOf(level.uppercase()) } catch (e: Exception) { ConsoleLogLevel.LOG }
        addConsoleLog(ConsoleLog(level = logLevel, message = message, sourceUrl = sourceUrl))
    }

    override fun onNetworkRequestReceived(
        url: String, method: String, statusCode: Int, type: String, durationMs: Long,
        reqHeaders: String, reqBody: String, resHeaders: String, resBody: String
    ) {
        val req = NetworkRequest(
            url = url,
            method = method,
            statusCode = statusCode,
            type = type,
            durationMs = durationMs,
            requestHeaders = mapOf("Raw" to reqHeaders),
            requestBody = reqBody,
            responseHeaders = mapOf("Raw" to resHeaders),
            responseBody = resBody,
            queryParams = parseQueryParams(url)
        )
        viewModelScope.launch(Dispatchers.Main) {
            _networkRequests.value = (listOf(req) + _networkRequests.value.filterNot { it.url == url && it.type == type }).take(150)
        }
    }

    override fun onNetworkRequestJsonReceived(jsonString: String) {
        try {
            val obj = JSONObject(jsonString)
            val url = obj.optString("url")
            if (url.isBlank()) return

            val reqHeadersMap = mutableMapOf<String, String>()
            val reqHeadersObj = obj.optJSONObject("requestHeaders")
            reqHeadersObj?.keys()?.forEach { k -> reqHeadersMap[k] = reqHeadersObj.getString(k) }

            val resHeadersMap = mutableMapOf<String, String>()
            val resHeadersObj = obj.optJSONObject("responseHeaders")
            resHeadersObj?.keys()?.forEach { k -> resHeadersMap[k] = resHeadersObj.getString(k) }

            val req = NetworkRequest(
                url = url,
                method = obj.optString("method", "GET"),
                statusCode = obj.optInt("statusCode", 200),
                statusText = obj.optString("statusText", "OK"),
                type = obj.optString("type", "fetch"),
                durationMs = obj.optLong("durationMs", 0),
                sizeBytes = obj.optLong("sizeBytes", 0),
                initiator = obj.optString("initiator", "Script"),
                requestHeaders = reqHeadersMap,
                requestBody = obj.optString("requestBody", ""),
                responseHeaders = resHeadersMap,
                responseBody = obj.optString("responseBody", ""),
                queryParams = parseQueryParams(url)
            )

            viewModelScope.launch(Dispatchers.Main) {
                val current = _networkRequests.value
                // Deduplicate if already recorded with same URL and type within last second
                val updated = listOf(req) + current.filterNot { it.url == req.url && it.type == req.type && (req.timestamp - it.timestamp) < 1000 }
                _networkRequests.value = updated.take(150)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onWebSocketFrameReceived(wsUrl: String, direction: String, payload: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val frame = WsFrame(direction = direction, payload = payload)
            val currentList = _networkRequests.value.toMutableList()
            val index = currentList.indexOfFirst { it.url == wsUrl && it.type == "ws" }
            if (index >= 0) {
                val existing = currentList[index]
                val updatedReq = existing.copy(wsFrames = existing.wsFrames + frame)
                currentList[index] = updatedReq
                _networkRequests.value = currentList
                if (_selectedNetworkRequest.value?.id == existing.id) {
                    _selectedNetworkRequest.value = updatedReq
                }
            } else {
                val newWsReq = NetworkRequest(
                    url = wsUrl,
                    method = "GET",
                    statusCode = 101,
                    statusText = "Switching Protocols",
                    type = "ws",
                    initiator = "WebSocket",
                    requestHeaders = mapOf("Upgrade" to "websocket", "Connection" to "Upgrade"),
                    responseHeaders = mapOf("HTTP/1.1" to "101 Switching Protocols"),
                    wsFrames = listOf(frame),
                    queryParams = parseQueryParams(wsUrl)
                )
                _networkRequests.value = listOf(newWsReq) + _networkRequests.value
            }
        }
    }

    private fun parseQueryParams(urlString: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val query = urlString.substringAfter("?", "")
            if (query.isNotBlank()) {
                query.split("&").forEach { param ->
                    val parts = param.split("=")
                    if (parts.size >= 2) {
                        map[java.net.URLDecoder.decode(parts[0], "UTF-8")] = java.net.URLDecoder.decode(parts.subList(1, parts.size).joinToString("="), "UTF-8")
                    } else if (parts.isNotEmpty()) {
                        map[java.net.URLDecoder.decode(parts[0], "UTF-8")] = ""
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return map
    }

    override fun onElementInspectedReceived(jsonString: String) {
        try {
            val obj = JSONObject(jsonString)
            val attrMap = mutableMapOf<String, String>()
            val attrsObj = obj.optJSONObject("attributes")
            attrsObj?.keys()?.forEach { k -> attrMap[k] = attrsObj.getString(k) }

            val styleMap = mutableMapOf<String, String>()
            val stylesObj = obj.optJSONObject("computedStyles")
            stylesObj?.keys()?.forEach { k -> styleMap[k] = stylesObj.getString(k) }

            val inspected = InspectedElement(
                tagName = obj.optString("tagName"),
                id = obj.optString("id"),
                className = obj.optString("className"),
                attributes = attrMap,
                computedStyles = styleMap,
                outerHtml = obj.optString("outerHtml"),
                innerHtml = obj.optString("innerHtml"),
                width = obj.optDouble("width").toFloat(),
                height = obj.optDouble("height").toFloat(),
                top = obj.optDouble("top").toFloat(),
                left = obj.optDouble("left").toFloat()
            )

            viewModelScope.launch(Dispatchers.Main) {
                _inspectedElement.value = inspected
                _isInspectorActive.value = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDomTreeExtractedReceived(jsonString: String) {
        try {
            val rootObj = JSONObject(jsonString)
            val parsed = parseDomNode(rootObj)
            viewModelScope.launch(Dispatchers.Main) {
                _domTree.value = parsed
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStorageExtractedReceived(
        localStorageJson: String,
        sessionStorageJson: String,
        cookieString: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val localList = parseStorageItems(localStorageJson)
            val sessionList = parseStorageItems(sessionStorageJson)
            val cookieList = parseCookieItems(cookieString)

            launch(Dispatchers.Main) {
                _localStorage.value = localList
                _sessionStorage.value = sessionList
                _cookies.value = cookieList
            }
        }
    }

    private fun addConsoleLog(log: ConsoleLog) {
        viewModelScope.launch(Dispatchers.Main) {
            _consoleLogs.value = (_consoleLogs.value + log).takeLast(200)
        }
    }

    private fun parseDomNode(obj: JSONObject): DomNode {
        val attrs = mutableMapOf<String, String>()
        val attrsObj = obj.optJSONObject("attributes")
        attrsObj?.keys()?.forEach { k -> attrs[k] = attrsObj.getString(k) }

        val children = mutableListOf<DomNode>()
        val childrenArr = obj.optJSONArray("children")
        if (childrenArr != null) {
            for (i in 0 until childrenArr.length()) {
                val childObj = childrenArr.getJSONObject(i)
                children.add(parseDomNode(childObj))
            }
        }

        return DomNode(
            nodeId = obj.optString("nodeId"),
            tagName = obj.optString("tagName"),
            isTextNode = obj.optBoolean("isTextNode"),
            textContent = obj.optString("textContent"),
            attributes = attrs,
            children = children
        )
    }

    private fun parseStorageItems(json: String): List<StorageItem> {
        val list = mutableListOf<StorageItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(StorageItem(key = obj.optString("key"), value = obj.optString("value")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseCookieItems(cookieHeader: String): List<CookieItem> {
        if (cookieHeader.isBlank()) return emptyList()
        val list = mutableListOf<CookieItem>()
        val parts = cookieHeader.split(";")
        for (part in parts) {
            val kv = part.trim().split("=")
            if (kv.size >= 2) {
                list.add(CookieItem(name = kv[0].trim(), value = kv.subList(1, kv.size).joinToString("=").trim()))
            }
        }
        return list
    }
}
