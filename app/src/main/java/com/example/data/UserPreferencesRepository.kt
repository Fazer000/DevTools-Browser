package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dev_browser_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_LAST_URL = stringPreferencesKey("last_url")
        private val KEY_DEVTOOLS_HEIGHT = floatPreferencesKey("devtools_height_fraction")
        private val KEY_DEVTOOLS_VISIBLE = booleanPreferencesKey("devtools_visible")
        private val KEY_USER_AGENT = stringPreferencesKey("user_agent_type")
        private val KEY_BOOKMARKS = stringPreferencesKey("bookmarks_json")
        private val KEY_HISTORY = stringPreferencesKey("history_json")
        private val KEY_JS_ENABLED = booleanPreferencesKey("js_enabled")
        private val KEY_CACHE_ENABLED = booleanPreferencesKey("cache_enabled")
        private val KEY_NET_SEARCH_QUERY = stringPreferencesKey("net_search_query")
        private val KEY_NET_EXCLUDE_QUERY = stringPreferencesKey("net_exclude_query")
        private val KEY_NET_FILTER_TYPE = stringPreferencesKey("net_filter_type")
        private val KEY_NET_SEARCH_BODY = booleanPreferencesKey("net_search_body")
        private val KEY_NET_REGEX_MODE = booleanPreferencesKey("net_regex_mode")
    }

    val lastUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_URL] ?: "https://example.com"
    }

    val devToolsHeightFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVTOOLS_HEIGHT] ?: 0.45f
    }

    val devToolsVisibleFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVTOOLS_VISIBLE] ?: true
    }

    val userAgentTypeFlow: Flow<UserAgentType> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_USER_AGENT] ?: UserAgentType.MOBILE_CHROME.name
        try {
            UserAgentType.valueOf(name)
        } catch (e: Exception) {
            UserAgentType.MOBILE_CHROME
        }
    }

    val jsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_JS_ENABLED] ?: true
    }

    val cacheEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CACHE_ENABLED] ?: true
    }

    val netSearchQueryFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_NET_SEARCH_QUERY] ?: ""
    }

    val netExcludeQueryFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_NET_EXCLUDE_QUERY] ?: ""
    }

    val netFilterTypeFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_NET_FILTER_TYPE]
    }

    val netSearchBodyFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NET_SEARCH_BODY] ?: false
    }

    val netRegexModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NET_REGEX_MODE] ?: false
    }

    val bookmarksFlow: Flow<List<BookmarkItem>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_BOOKMARKS] ?: "[]"
        parseBookmarks(jsonStr)
    }

    val historyFlow: Flow<List<HistoryItem>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_HISTORY] ?: "[]"
        parseHistory(jsonStr)
    }

    suspend fun saveLastUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_URL] = url
        }
    }

    suspend fun saveDevToolsHeight(fraction: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVTOOLS_HEIGHT] = fraction.coerceIn(0.15f, 0.85f)
        }
    }

    suspend fun saveDevToolsVisible(visible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVTOOLS_VISIBLE] = visible
        }
    }

    suspend fun saveUserAgentType(userAgentType: UserAgentType) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_AGENT] = userAgentType.name
        }
    }

    suspend fun saveJsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_JS_ENABLED] = enabled
        }
    }

    suspend fun saveCacheEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CACHE_ENABLED] = enabled
        }
    }

    suspend fun saveNetworkSearchQuery(query: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NET_SEARCH_QUERY] = query
        }
    }

    suspend fun saveNetworkExcludeQuery(query: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NET_EXCLUDE_QUERY] = query
        }
    }

    suspend fun saveNetworkFilterType(type: String?) {
        context.dataStore.edit { prefs ->
            if (type == null) {
                prefs.remove(KEY_NET_FILTER_TYPE)
            } else {
                prefs[KEY_NET_FILTER_TYPE] = type
            }
        }
    }

    suspend fun saveNetworkSearchBody(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NET_SEARCH_BODY] = enabled
        }
    }

    suspend fun saveNetworkRegexMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NET_REGEX_MODE] = enabled
        }
    }

    suspend fun addBookmark(title: String, url: String) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]").toMutableList()
            if (current.none { it.url == url }) {
                current.add(0, BookmarkItem(title = title, url = url))
                prefs[KEY_BOOKMARKS] = serializeBookmarks(current)
            }
        }
    }

    suspend fun removeBookmark(url: String) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]").toMutableList()
            current.removeAll { it.url == url }
            prefs[KEY_BOOKMARKS] = serializeBookmarks(current)
        }
    }

    suspend fun addHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("data:") || url.startsWith("about:")) return
        context.dataStore.edit { prefs ->
            val current = parseHistory(prefs[KEY_HISTORY] ?: "[]").toMutableList()
            current.removeAll { it.url == url }
            current.add(0, HistoryItem(title = if (title.isBlank()) url else title, url = url))
            if (current.size > 100) {
                current.removeAt(current.lastIndex)
            }
            prefs[KEY_HISTORY] = serializeHistory(current)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HISTORY] = "[]"
        }
    }

    private fun parseBookmarks(json: String): List<BookmarkItem> {
        val list = mutableListOf<BookmarkItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BookmarkItem(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (list.isEmpty()) {
            // Default developer bookmarks
            list.add(BookmarkItem(title = "Example Domain", url = "https://example.com"))
            list.add(BookmarkItem(title = "GitHub", url = "https://github.com"))
            list.add(BookmarkItem(title = "Google Search", url = "https://google.com"))
            list.add(BookmarkItem(title = "HTTPBin Test API", url = "https://httpbin.org"))
            list.add(BookmarkItem(title = "Flexbox Froggy", url = "https://flexboxfroggy.com"))
        }
        return list
    }

    private fun serializeBookmarks(list: List<BookmarkItem>): String {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("timestamp", item.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseHistory(json: String): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeHistory(list: List<HistoryItem>): String {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("timestamp", item.timestamp)
            array.put(obj)
        }
        return array.toString()
    }
}
