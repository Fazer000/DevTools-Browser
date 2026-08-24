package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ContentCopy
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NetworkRequest
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.BrowserViewModel

@Composable
fun NetworkTab(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val requests by viewModel.networkRequests.collectAsState()
    val selectedType by viewModel.networkFilterType.collectAsState()
    val selectedRequest by viewModel.selectedNetworkRequest.collectAsState()
    val searchQuery by viewModel.networkSearchQuery.collectAsState()
    val excludeQuery by viewModel.networkExcludeQuery.collectAsState()
    val searchBody by viewModel.networkSearchBody.collectAsState()
    val regexMode by viewModel.networkRegexMode.collectAsState()

    NetworkTabContent(
        requests = requests,
        selectedType = selectedType,
        selectedRequest = selectedRequest,
        searchQuery = searchQuery,
        excludeQuery = excludeQuery,
        searchBody = searchBody,
        regexMode = regexMode,
        onSelectType = viewModel::setNetworkFilterType,
        onSelectRequest = viewModel::selectNetworkRequest,
        onSearchQueryChange = viewModel::setNetworkSearchQuery,
        onExcludeQueryChange = viewModel::setNetworkExcludeQuery,
        onAddExcludeWord = viewModel::addExcludeWord,
        onRemoveExcludeWord = viewModel::removeExcludeWord,
        onSearchBodyToggle = viewModel::setNetworkSearchBody,
        onRegexModeToggle = viewModel::setNetworkRegexMode,
        onClearNetworkFilters = viewModel::clearNetworkFilters,
        onClearNetwork = viewModel::clearNetworkLogs,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkTabContent(
    requests: List<NetworkRequest>,
    selectedType: String?,
    selectedRequest: NetworkRequest?,
    searchQuery: String,
    excludeQuery: String,
    searchBody: Boolean,
    regexMode: Boolean,
    onSelectType: (String?) -> Unit,
    onSelectRequest: (NetworkRequest?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onExcludeQueryChange: (String) -> Unit,
    onAddExcludeWord: (String) -> Unit,
    onRemoveExcludeWord: (String) -> Unit,
    onSearchBodyToggle: (Boolean) -> Unit,
    onRegexModeToggle: (Boolean) -> Unit,
    onClearNetworkFilters: () -> Unit,
    onClearNetwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFilterDrawerOpen by remember { mutableStateOf(false) }
    var newExcludeWordInput by remember { mutableStateOf("") }

    val filterCategories = remember {
        listOf(
            "All" to null,
            "Fetch/XHR" to "fetch_xhr",
            "JS" to "js",
            "CSS" to "css",
            "Img" to "img",
            "Media" to "media",
            "Font" to "font",
            "Doc" to "doc",
            "WS" to "ws",
            "Other" to "other"
        )
    }

    // Parsed list of active exclude words for pill chips
    val activeExcludeWords = remember(excludeQuery) {
        excludeQuery.split(",")
            .map { it.trim().trimStart('-', '!', ',') }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // Advanced Filtering Logic
    val filteredRequests = remember(requests, selectedType, searchQuery, excludeQuery, searchBody, regexMode) {
        requests.filter { req ->
            matchesNetworkRequest(
                req = req,
                category = selectedType,
                searchQuery = searchQuery,
                excludeQuery = excludeQuery,
                searchBody = searchBody,
                regexMode = regexMode
            )
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Smart scroll tracking: user is at top if viewing index 0 near top offset
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 30
        }
    }

    val topRequestId = filteredRequests.firstOrNull()?.id

    // Smart stick-to-top auto-scroll: ONLY when user is already at top
    LaunchedEffect(topRequestId) {
        if (isAtTop && topRequestId != null && filteredRequests.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val excludedCount = requests.size - filteredRequests.size
    val isAnyFilterActive = searchQuery.isNotBlank() || excludeQuery.isNotBlank() || selectedType != null || searchBody || regexMode

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top Search Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Main Search Input
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("network_search_input"),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search (e.g. api -analytics status:200)...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Exclude / Filter Options Drawer Toggle Button
            IconButton(
                onClick = { isFilterDrawerOpen = !isFilterDrawerOpen },
                modifier = Modifier.size(32.dp)
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter Options",
                        tint = if (isFilterDrawerOpen || activeExcludeWords.isNotEmpty() || searchBody || regexMode)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeExcludeWords.isNotEmpty() || searchBody || regexMode) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
            }

            // Clear Logs Button
            IconButton(onClick = onClearNetwork, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Logs",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expandable Filter & Exclude Panel
        AnimatedVisibility(
            visible = isFilterDrawerOpen,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Exclusion & Advanced Filters",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Add Exclude Word Input Field
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Exclude",
                                    tint = StatusError,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                BasicTextField(
                                    value = newExcludeWordInput,
                                    onValueChange = { newExcludeWordInput = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (newExcludeWordInput.isNotBlank()) {
                                            onAddExcludeWord(newExcludeWordInput)
                                            newExcludeWordInput = ""
                                        }
                                    }),
                                    textStyle = LocalTextStyle.current.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp
                                    ),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (newExcludeWordInput.isEmpty()) {
                                                Text(
                                                    text = "Add exclude word (e.g. analytics, png)...",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (newExcludeWordInput.isNotBlank()) {
                                    onAddExcludeWord(newExcludeWordInput)
                                    newExcludeWordInput = ""
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Exclude", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Exclude", fontSize = 11.sp)
                        }
                    }

                    // Active Exclude Word Chips List
                    if (activeExcludeWords.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Active Excludes: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(activeExcludeWords) { word ->
                                    Surface(
                                        color = StatusError.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "- $word",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusError
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Remove $word",
                                                tint = StatusError,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable { onRemoveExcludeWord(word) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Presets:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            item {
                                QuickPresetChip(label = "+ Analytics", onClick = { onAddExcludeWord("analytics") })
                            }
                            item {
                                QuickPresetChip(label = "+ Images", onClick = { onAddExcludeWord("png"); onAddExcludeWord("jpg"); onAddExcludeWord("svg") })
                            }
                            item {
                                QuickPresetChip(label = "+ Static CSS/JS", onClick = { onAddExcludeWord("css"); onAddExcludeWord("js") })
                            }
                            item {
                                QuickPresetChip(label = "Errors Only", onClick = { onSearchQueryChange("is:error") })
                            }
                        }
                    }

                    // Toggle Switches (Search in Body & Regex)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = searchBody,
                                onCheckedChange = onSearchBodyToggle,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search Headers & Body", fontSize = 11.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = regexMode,
                                onCheckedChange = onRegexModeToggle,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Regex Mode", fontSize = 11.sp)
                        }
                    }

                    if (isAnyFilterActive) {
                        TextButton(
                            onClick = onClearNetworkFilters,
                            modifier = Modifier.align(Alignment.End).height(28.dp)
                        ) {
                            Text("Reset All Filters", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Horizontal Category Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filterCategories) { (label, categoryValue) ->
                val isSelected = selectedType == categoryValue
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectType(categoryValue) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // Filter Stats Status Bar
        if (isAnyFilterActive || requests.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAnyFilterActive)
                        "Showing ${filteredRequests.size} of ${requests.size} requests ($excludedCount filtered)"
                    else "${requests.size} requests recorded",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isAnyFilterActive) {
                    Text(
                        text = "Clear filters",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onClearNetworkFilters() }
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Requests List
        if (filteredRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAnyFilterActive) "No matching network requests found." else "No network requests recorded yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAnyFilterActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = onClearNetworkFilters) {
                            Text("Reset search & exclude filters", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current

            // Table Column Headers Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(42.dp))
                    Text("METHOD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
                    Text("NAME / PATH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("TYPE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
                    Text("TIME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp))
                    Text("COPY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(28.dp))
                }
            }
            HorizontalDivider()

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("network_requests_list")
                ) {
                    items(filteredRequests, key = { it.id }) { req ->
                        val statusColor = when {
                            req.statusCode in 200..299 -> StatusSuccess
                            req.statusCode == 101 -> StatusInfo
                            req.statusCode in 300..399 -> StatusWarning
                            else -> StatusError
                        }
                        val displayPath = remember(req.url) { extractPathFromUrl(req.url) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectRequest(req) },
                            color = if (selectedRequest?.id == req.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Status Badge
                                Box(modifier = Modifier.width(42.dp), contentAlignment = Alignment.CenterStart) {
                                    Surface(
                                        color = statusColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (req.statusCode == 0) "FAIL" else req.statusCode.toString(),
                                            color = statusColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Method Badge
                                Text(
                                    text = req.method,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(48.dp)
                                )

                                // Relative URL Path (Domain Removed)
                                Text(
                                    text = displayPath,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Type Tag
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.width(40.dp)
                                ) {
                                    Text(
                                        text = req.type.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                        maxLines = 1
                                    )
                                }

                                // Timing
                                Text(
                                    text = if (req.durationMs > 0) "${req.durationMs}ms" else "-",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(44.dp)
                                )

                                // Copy URL Icon Button
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(req.url))
                                        android.widget.Toast.makeText(context, "Copied URL: ${req.url}", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy URL",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }

                // Quick Floating Scroll To Top Button if user scrolled down
                if (!isAtTop && filteredRequests.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Scroll to top",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Full Request Details Bottom Sheet when request is selected
        selectedRequest?.let { req ->
            NetworkRequestDetailsSheet(
                request = req,
                onDismiss = { onSelectRequest(null) }
            )
        }
    }
}

private fun extractPathFromUrl(urlString: String): String {
    if (urlString.isBlank()) return "/"
    return try {
        val uri = java.net.URI(urlString)
        val path = uri.rawPath.ifEmpty { "/" }
        val query = uri.rawQuery
        if (!query.isNullOrBlank()) "$path?$query" else path
    } catch (e: Exception) {
        val withoutScheme = urlString.substringAfter("://")
        val firstSlash = withoutScheme.indexOf('/')
        if (firstSlash != -1) withoutScheme.substring(firstSlash) else "/"
    }
}

@Composable
private fun QuickPresetChip(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// Request Matching Algorithm
private fun matchesNetworkRequest(
    req: NetworkRequest,
    category: String?,
    searchQuery: String,
    excludeQuery: String,
    searchBody: Boolean,
    regexMode: Boolean
): Boolean {
    // 1. Category Filter
    val categoryMatches = when (category) {
        null -> true
        "fetch_xhr" -> req.type.equals("fetch", ignoreCase = true) || req.type.equals("xhr", ignoreCase = true)
        else -> req.type.equals(category, ignoreCase = true)
    }
    if (!categoryMatches) return false

    // Fast path: if search and exclude queries are empty, match category immediately
    if (searchQuery.isBlank() && excludeQuery.isBlank()) {
        return true
    }

    // Parse exclude words from excludeQuery box (comma or space separated)
    val excludeWordsList = excludeQuery
        .split(",", " ")
        .map { it.trim().trimStart('-', '!', ',') }
        .filter { it.isNotBlank() }

    // Parse search tokens from main searchQuery box
    val tokens = searchQuery.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val inlineExcludes = mutableListOf<String>()
    val positiveTokens = mutableListOf<String>()

    for (token in tokens) {
        if (token.startsWith("-") || token.startsWith("!")) {
            val word = token.substring(1).trim()
            if (word.isNotBlank()) inlineExcludes.add(word)
        } else {
            positiveTokens.add(token)
        }
    }

    val allExcludes = (excludeWordsList + inlineExcludes).distinct()

    // Build searchable target string
    val searchable = buildString {
        append(req.url).append(" ")
        append(req.method).append(" ")
        append(req.statusCode).append(" ")
        append(req.statusText).append(" ")
        append(req.type).append(" ")
        append(req.initiator).append(" ")
        if (searchBody) {
            append(req.requestHeaders.toString()).append(" ")
            append(req.responseHeaders.toString()).append(" ")
            append(req.requestBody).append(" ")
            append(req.responseBody).append(" ")
            req.queryParams.forEach { (k, v) -> append(k).append("=").append(v).append(" ") }
        }
    }

    // 2. Evaluate Exclude Patterns (If ANY match, REJECT request)
    for (exclude in allExcludes) {
        if (exclude.startsWith("status:", ignoreCase = true)) {
            val expectedCode = exclude.substringAfter("status:").trim()
            if (matchesStatusCode(req.statusCode, expectedCode)) return false
        } else if (exclude.startsWith("method:", ignoreCase = true)) {
            val expectedMethod = exclude.substringAfter("method:").trim()
            if (req.method.equals(expectedMethod, ignoreCase = true)) return false
        } else if (exclude.startsWith("domain:", ignoreCase = true)) {
            val domain = exclude.substringAfter("domain:").trim()
            if (req.url.contains(domain, ignoreCase = true)) return false
        } else if (regexMode) {
            try {
                if (Regex(exclude, RegexOption.IGNORE_CASE).containsMatchIn(searchable)) return false
            } catch (_: Exception) {
                if (searchable.contains(exclude, ignoreCase = true)) return false
            }
        } else {
            if (searchable.contains(exclude, ignoreCase = true)) return false
        }
    }

    // 3. Evaluate Positive Search Tokens (ALL must match)
    for (pos in positiveTokens) {
        if (pos.startsWith("status:", ignoreCase = true)) {
            val code = pos.substringAfter("status:").trim()
            if (!matchesStatusCode(req.statusCode, code)) return false
        } else if (pos.startsWith("method:", ignoreCase = true)) {
            val method = pos.substringAfter("method:").trim()
            if (!req.method.equals(method, ignoreCase = true)) return false
        } else if (pos.startsWith("domain:", ignoreCase = true)) {
            val domain = pos.substringAfter("domain:").trim()
            if (!req.url.contains(domain, ignoreCase = true)) return false
        } else if (pos.startsWith("type:", ignoreCase = true)) {
            val t = pos.substringAfter("type:").trim()
            if (!req.type.contains(t, ignoreCase = true)) return false
        } else if (pos.equals("is:error", ignoreCase = true)) {
            if (req.statusCode in 200..399 || req.statusCode == 101) return false
        } else if (pos.equals("is:ws", ignoreCase = true)) {
            if (!req.type.equals("ws", ignoreCase = true)) return false
        } else if (regexMode) {
            try {
                if (!Regex(pos, RegexOption.IGNORE_CASE).containsMatchIn(searchable)) return false
            } catch (_: Exception) {
                if (!searchable.contains(pos, ignoreCase = true)) return false
            }
        } else {
            if (!searchable.contains(pos, ignoreCase = true)) return false
        }
    }

    return true
}

private fun matchesStatusCode(actualCode: Int, pattern: String): Boolean {
    if (pattern.endsWith("xx", ignoreCase = true)) {
        val hundred = pattern.substring(0, 1).toIntOrNull() ?: return false
        return actualCode / 100 == hundred
    }
    if (pattern.equals("error", ignoreCase = true)) {
        return actualCode == 0 || actualCode >= 400
    }
    return actualCode.toString() == pattern
}
