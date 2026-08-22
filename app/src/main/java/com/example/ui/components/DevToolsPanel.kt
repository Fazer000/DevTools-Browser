package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.BrowserViewModel

@Composable
fun DevToolsPanel(
    viewModel: BrowserViewModel,
    totalHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeDevToolsTab.collectAsState()
    val domTree by viewModel.domTree.collectAsState()
    val inspectedElement by viewModel.inspectedElement.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()
    val consoleFilterLevel by viewModel.consoleFilterLevel.collectAsState()
    val consoleSearchQuery by viewModel.consoleSearchQuery.collectAsState()
    val networkRequests by viewModel.networkRequests.collectAsState()
    val networkFilterType by viewModel.networkFilterType.collectAsState()
    val selectedNetworkRequest by viewModel.selectedNetworkRequest.collectAsState()
    val pageSource by viewModel.pageSource.collectAsState()
    val cookies by viewModel.cookies.collectAsState()
    val localStorage by viewModel.localStorage.collectAsState()
    val sessionStorage by viewModel.sessionStorage.collectAsState()
    val userAgentType by viewModel.userAgentType.collectAsState()
    val jsEnabled by viewModel.jsEnabled.collectAsState()
    val cacheEnabled by viewModel.cacheEnabled.collectAsState()

    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Draggable Resizer Splitter Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(totalHeightPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (totalHeightPx > 0) {
                            val dy = dragAmount.y
                            val currentFraction = viewModel.devToolsHeightFraction.value
                            val deltaFraction = -dy / totalHeightPx
                            viewModel.updateDevToolsHeight(currentFraction + deltaFraction)
                        }
                    }
                }
                .testTag("devtools_resizer_handle"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }

            IconButton(
                onClick = { viewModel.toggleDevToolsVisibility() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close DevTools",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // DevTools Header Tabs Scrollable Row
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            DevToolsTab.entries.forEach { tab ->
                val badgeCount = when (tab) {
                    DevToolsTab.CONSOLE -> consoleLogs.size
                    DevToolsTab.NETWORK -> networkRequests.size
                    else -> 0
                }

                Tab(
                    selected = activeTab == tab,
                    onClick = { viewModel.setDevToolsTab(tab) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                            if (badgeCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    color = if (activeTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                        fontSize = 10.sp,
                                        color = if (activeTab == tab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Tab Content Switcher
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                DevToolsTab.ELEMENTS -> {
                    ElementsTab(
                        domTree = domTree,
                        inspectedElement = inspectedElement,
                        onToggleInspector = { viewModel.toggleInspector() }
                    )
                }
                DevToolsTab.CONSOLE -> {
                    ConsoleTab(
                        logs = consoleLogs,
                        selectedLevel = consoleFilterLevel,
                        searchQuery = consoleSearchQuery,
                        onSelectLevel = { viewModel.setConsoleFilterLevel(it) },
                        onSearchQueryChange = { viewModel.setConsoleSearchQuery(it) },
                        onExecuteJs = { viewModel.executeJsInConsole(it) },
                        onClearConsole = { viewModel.clearConsole() }
                    )
                }
                DevToolsTab.NETWORK -> {
                    NetworkTab(
                        requests = networkRequests,
                        selectedType = networkFilterType,
                        selectedRequest = selectedNetworkRequest,
                        onSelectType = { viewModel.setNetworkFilterType(it) },
                        onSelectRequest = { viewModel.selectNetworkRequest(it) },
                        onClearNetwork = { viewModel.clearNetworkLogs() }
                    )
                }
                DevToolsTab.SOURCES -> {
                    SourcesTab(pageSource = pageSource)
                }
                DevToolsTab.STORAGE -> {
                    StorageTab(
                        cookies = cookies,
                        localStorage = localStorage,
                        sessionStorage = sessionStorage
                    )
                }
                DevToolsTab.DEVICES -> {
                    DevicesSettingsTab(
                        currentUserAgent = userAgentType,
                        jsEnabled = jsEnabled,
                        cacheEnabled = cacheEnabled,
                        onSelectUserAgent = { viewModel.setUserAgent(it) },
                        onSetJsEnabled = { viewModel.setJsEnabled(it) },
                        onSetCacheEnabled = { viewModel.setCacheEnabled(it) },
                        onReload = { viewModel.reload() }
                    )
                }
            }
        }
    }
}
