package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NetworkRequest
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkRequestDetailsSheet(
    request: NetworkRequest,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var activeSubTab by remember { mutableStateOf("Headers") }
    var copiedNotification by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedNotification) {
        if (copiedNotification != null) {
            kotlinx.coroutines.delay(2000)
            copiedNotification = null
        }
    }

    val availableTabs = remember(request) {
        val list = mutableListOf("Headers", "Payload", "Preview", "Response")
        if (request.type.equals("ws", ignoreCase = true) || request.wsFrames.isNotEmpty()) {
            list.add("Messages")
        }
        list.add("Timing")
        list
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
                .testTag("network_details_sheet")
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusColor = when {
                            request.statusCode in 200..299 -> StatusSuccess
                            request.statusCode == 101 -> StatusInfo
                            request.statusCode in 300..399 -> StatusWarning
                            else -> StatusError
                        }
                        Surface(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${request.method} ${if (request.statusCode == 0) "FAIL" else request.statusCode}",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = request.type.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = request.url,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-Tabs Row
            ScrollableTabRow(
                selectedTabIndex = availableTabs.indexOf(activeSubTab).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }
            ) {
                availableTabs.forEach { tabName ->
                    Tab(
                        selected = activeSubTab == tabName,
                        onClick = { activeSubTab = tabName },
                        text = {
                            Text(
                                text = tabName,
                                fontSize = 12.sp,
                                fontWeight = if (activeSubTab == tabName) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Contents
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeSubTab) {
                    "Headers" -> HeadersTabContent(request, onCopy = { text, label ->
                        clipboardManager.setText(AnnotatedString(text))
                        copiedNotification = "$label copied!"
                    })
                    "Payload" -> PayloadTabContent(request, onCopy = { text, label ->
                        clipboardManager.setText(AnnotatedString(text))
                        copiedNotification = "$label copied!"
                    })
                    "Preview" -> PreviewTabContent(request)
                    "Response" -> ResponseTabContent(request, onCopy = { text, label ->
                        clipboardManager.setText(AnnotatedString(text))
                        copiedNotification = "$label copied!"
                    })
                    "Messages" -> WsMessagesTabContent(request)
                    "Timing" -> TimingTabContent(request)
                }
            }

            // Copy Toast Snackbar Feedback
            copiedNotification?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeadersTabContent(request: NetworkRequest, onCopy: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderSectionTitle("General Information")
            HeaderInfoRow("Request URL", request.url)
            HeaderInfoRow("Request Method", request.method)
            HeaderInfoRow("Status Code", "${request.statusCode} ${request.statusText}")
            HeaderInfoRow("Initiator", request.initiator)
            HeaderInfoRow("Duration", "${request.durationMs} ms")
            HeaderInfoRow("Size", if (request.sizeBytes > 0) "${request.sizeBytes} Bytes" else "Unknown")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderSectionTitle("Response Headers (${request.responseHeaders.size})")
                if (request.responseHeaders.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val str = request.responseHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                            onCopy(str, "Response Headers")
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Response Headers", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (request.responseHeaders.isEmpty()) {
                Text("[No response headers recorded]", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                request.responseHeaders.forEach { (k, v) ->
                    HeaderKeyValueRow(key = k, value = v)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderSectionTitle("Request Headers (${request.requestHeaders.size})")
                if (request.requestHeaders.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val str = request.requestHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                            onCopy(str, "Request Headers")
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Request Headers", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (request.requestHeaders.isEmpty()) {
                Text("[No request headers recorded]", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                request.requestHeaders.forEach { (k, v) ->
                    HeaderKeyValueRow(key = k, value = v)
                }
            }
        }
    }
}

@Composable
private fun PayloadTabContent(request: NetworkRequest, onCopy: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (request.queryParams.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderSectionTitle("Query String Parameters (${request.queryParams.size})")
                    IconButton(
                        onClick = {
                            val str = request.queryParams.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                            onCopy(str, "Query Parameters")
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Query Parameters", modifier = Modifier.size(16.dp))
                    }
                }

                request.queryParams.forEach { (k, v) ->
                    HeaderKeyValueRow(key = k, value = v)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderSectionTitle("Request Body")
                if (request.requestBody.isNotBlank()) {
                    IconButton(
                        onClick = { onCopy(request.requestBody, "Request Body") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Request Body", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (request.requestBody.isBlank()) {
                Text("[No Request Body]", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                CodeBlock(request.requestBody)
            }
        }
    }
}

@Composable
private fun PreviewTabContent(request: NetworkRequest) {
    val body = request.responseBody
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            HeaderSectionTitle("Response Preview")
            if (body.isBlank()) {
                Text("[No Preview Available]", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                CodeBlock(body)
            }
        }
    }
}

@Composable
private fun ResponseTabContent(request: NetworkRequest, onCopy: (String, String) -> Unit) {
    val body = request.responseBody
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Raw Response (${body.length} chars)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )

            if (body.isNotBlank()) {
                IconButton(onClick = { onCopy(body, "Response Body") }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Response", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (body.isBlank()) {
            Text("[Empty Response Body]", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            CodeBlock(body, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WsMessagesTabContent(request: NetworkRequest) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderSectionTitle("WebSocket Frame Stream (${request.wsFrames.size} messages)")

        if (request.wsFrames.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No WebSocket frames captured yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(request.wsFrames, key = { it.id }) { frame ->
                    val isSent = frame.direction.equals("sent", ignoreCase = true)
                    Surface(
                        color = if (isSent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (isSent) StatusInfo else StatusSuccess,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isSent) "OUT" else "IN",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = frame.payload,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingTabContent(request: NetworkRequest) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeaderSectionTitle("Request Duration & Timing")

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Duration", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${request.durationMs} ms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                // Waterfall bar visual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Transfer Size", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (request.sizeBytes > 0) "${request.sizeBytes} Bytes" else "N/A", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun HeaderSectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun HeaderInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(text = value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun HeaderKeyValueRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun CodeBlock(content: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
    ) {
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            item {
                Text(
                    text = content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
