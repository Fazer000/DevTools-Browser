package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserAgentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopControlBar(
    url: String,
    pageTitle: String,
    isLoading: Boolean,
    progress: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isDevToolsVisible: Boolean,
    isInspectorActive: Boolean,
    tabCount: Int,
    userAgentType: UserAgentType,
    isBookmarked: Boolean,
    onNavigate: (String) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onReload: () -> Unit,
    onStopLoading: () -> Unit,
    onToggleInspector: () -> Unit,
    onToggleDevTools: () -> Unit,
    onOpenTabsDialog: () -> Unit,
    onOpenBookmarksDrawer: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSelectUserAgent: (UserAgentType) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(url) { mutableStateOf(url) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var showMoreMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        // Upper Navigation & Address Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Back Button
            IconButton(
                onClick = onGoBack,
                enabled = canGoBack,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Forward Button
            IconButton(
                onClick = onGoForward,
                enabled = canGoForward,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_forward")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Refresh / Stop Button
            IconButton(
                onClick = if (isLoading) onStopLoading else onReload,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_reload")
            ) {
                Icon(
                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = if (isLoading) "Stop" else "Reload",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Address Bar Box
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .border(
                        width = 1.dp,
                        color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(21.dp)
                    ),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Security Icon (HTTPS / HTTP)
                    Icon(
                        imageVector = if (url.startsWith("https://")) Icons.Default.Lock else Icons.Outlined.Warning,
                        contentDescription = "Security Status",
                        tint = if (url.startsWith("https://")) Color(0xFF34D399) else Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Field
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .testTag("address_bar_input"),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                onNavigate(textFieldValue)
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (textFieldValue.isEmpty() && !isFocused) {
                                    Text(
                                        text = "Enter URL or search query...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // Clear Text Button
                    if (isFocused && textFieldValue.isNotBlank()) {
                        IconButton(
                            onClick = { textFieldValue = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // More Menu Button
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_more_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isBookmarked) "Remove Bookmark" else "Bookmark Page") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (isBookmarked) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            onToggleBookmark()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Bookmarks & History") },
                        leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            onOpenBookmarksDrawer()
                        }
                    )

                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text("User Agent: ${userAgentType.label}") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (userAgentType == UserAgentType.DESKTOP_CHROME || userAgentType == UserAgentType.DESKTOP_FIREFOX)
                                    Icons.Default.Computer else Icons.Default.Smartphone,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            val nextAgent = if (userAgentType == UserAgentType.MOBILE_CHROME) UserAgentType.DESKTOP_CHROME else UserAgentType.MOBILE_CHROME
                            onSelectUserAgent(nextAgent)
                        }
                    )
                }
            }
        }

        // Lower Developer Quick Tools Toolbar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inspect Element Crosshair Tool
                FilterChip(
                    selected = isInspectorActive,
                    onClick = onToggleInspector,
                    label = { Text("Inspect", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CenterFocusWeak,
                            contentDescription = "Inspect Element",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("chip_inspect_element")
                )

                // DevTools Panel Toggle
                FilterChip(
                    selected = isDevToolsVisible,
                    onClick = onToggleDevTools,
                    label = { Text("DevTools", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "DevTools Panel",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("chip_devtools_panel")
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bookmarks Button
                IconButton(
                    onClick = onOpenBookmarksDrawer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bookmarks,
                        contentDescription = "Bookmarks",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Tabs Switcher Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenTabsDialog),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabCount.toString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Live Page Load Progress Bar
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}
