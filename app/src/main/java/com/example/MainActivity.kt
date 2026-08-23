package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.DevBrowserTheme
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            DevBrowserTheme {
                val currentUrl by viewModel.currentUrl.collectAsState()
                val pageTitle by viewModel.pageTitle.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val progress by viewModel.loadProgress.collectAsState()
                val canGoBack by viewModel.canGoBack.collectAsState()
                val canGoForward by viewModel.canGoForward.collectAsState()
                val isDevToolsVisible by viewModel.isDevToolsVisible.collectAsState()
                val devToolsHeightFraction by viewModel.devToolsHeightFraction.collectAsState()
                val isInspectorActive by viewModel.isInspectorActive.collectAsState()
                val userAgentType by viewModel.userAgentType.collectAsState()
                val bookmarks by viewModel.bookmarks.collectAsState()
                val history by viewModel.history.collectAsState()
                val tabs by viewModel.tabs.collectAsState()
                val activeTabId by viewModel.activeTabId.collectAsState()
                val isBookmarksDrawerOpen by viewModel.isBookmarksDrawerOpen.collectAsState()
                val isTabsDialogOpen by viewModel.isTabsDialogOpen.collectAsState()

                val isBookmarked = remember(bookmarks, currentUrl) {
                    bookmarks.any { it.url == currentUrl }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopControlBar(
                            url = currentUrl,
                            pageTitle = pageTitle,
                            isLoading = isLoading,
                            progress = progress,
                            canGoBack = canGoBack,
                            canGoForward = canGoForward,
                            isDevToolsVisible = isDevToolsVisible,
                            isInspectorActive = isInspectorActive,
                            tabCount = tabs.size,
                            userAgentType = userAgentType,
                            isBookmarked = isBookmarked,
                            onNavigate = { viewModel.navigateToUrl(it) },
                            onGoBack = { viewModel.goBack() },
                            onGoForward = { viewModel.goForward() },
                            onReload = { viewModel.reload() },
                            onStopLoading = { viewModel.stopLoading() },
                            onToggleInspector = { viewModel.toggleInspector() },
                            onToggleDevTools = { viewModel.toggleDevToolsVisibility() },
                            onOpenTabsDialog = { viewModel.setTabsDialogOpen(true) },
                            onOpenBookmarksDrawer = { viewModel.setBookmarksDrawerOpen(true) },
                            onToggleBookmark = { viewModel.toggleBookmarkCurrentPage() },
                            onSelectUserAgent = { viewModel.setUserAgent(it) }
                        )
                    }
                ) { innerPadding ->
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        val totalHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top WebView Component
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(if (isDevToolsVisible) (1f - devToolsHeightFraction).coerceIn(0.15f, 0.85f) else 1f)
                            ) {
                                WebViewContainer(viewModel = viewModel)
                            }

                            // Bottom DevTools Panel Component (Split View)
                            if (isDevToolsVisible) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(devToolsHeightFraction.coerceIn(0.15f, 0.85f))
                                ) {
                                    DevToolsPanel(
                                        viewModel = viewModel,
                                        totalHeightPx = totalHeightPx
                                    )
                                }
                            }
                        }
                    }

                    // Modals
                    if (isBookmarksDrawerOpen) {
                        BookmarksHistoryModalSheet(
                            bookmarks = bookmarks,
                            history = history,
                            onSelectUrl = { viewModel.navigateToUrl(it) },
                            onRemoveBookmark = { viewModel.removeBookmark(it) },
                            onClearHistory = { viewModel.clearHistory() },
                            onDismiss = { viewModel.setBookmarksDrawerOpen(false) }
                        )
                    }

                    if (isTabsDialogOpen) {
                        TabsModalSheet(
                            tabs = tabs,
                            activeTabId = activeTabId,
                            onSwitchTab = { viewModel.switchTab(it) },
                            onCloseTab = { viewModel.closeTab(it) },
                            onAddNewTab = { viewModel.addNewTab() },
                            onDismiss = { viewModel.setTabsDialogOpen(false) }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val dataUri = intent.data
        if (action == Intent.ACTION_VIEW && dataUri != null) {
            val urlString = dataUri.toString()
            if (urlString.isNotBlank()) {
                viewModel.navigateToUrl(urlString)
            }
        }
    }
}
