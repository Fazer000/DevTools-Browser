package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NetworkRequest
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkTab(
    requests: List<NetworkRequest>,
    selectedType: String?,
    selectedRequest: NetworkRequest?,
    onSelectType: (String?) -> Unit,
    onSelectRequest: (NetworkRequest?) -> Unit,
    onClearNetwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredRequests = remember(requests, selectedType) {
        if (selectedType == null) requests
        else requests.filter { it.type.equals(selectedType, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Filter Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { onSelectType(null) },
                    label = { Text("All (${requests.size})", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = selectedType == "fetch",
                    onClick = { onSelectType(if (selectedType == "fetch") null else "fetch") },
                    label = { Text("Fetch", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = selectedType == "xhr",
                    onClick = { onSelectType(if (selectedType == "xhr") null else "xhr") },
                    label = { Text("XHR", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
            }

            IconButton(onClick = onClearNetwork, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

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
                        text = "No network requests recorded yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("network_requests_list")
            ) {
                items(filteredRequests, key = { it.id }) { req ->
                    val statusColor = when {
                        req.statusCode in 200..299 -> StatusSuccess
                        req.statusCode in 300..399 -> StatusWarning
                        else -> StatusError
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRequest(req) },
                        color = if (selectedRequest?.id == req.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Status Badge
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

                            // Method Badge
                            Text(
                                text = req.method,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // URL Path
                            Text(
                                text = req.url,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Type & Timing
                            Text(
                                text = "${req.type} | ${req.durationMs}ms",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }

        // Selected Request Detail Dialog/Modal
        if (selectedRequest != null) {
            AlertDialog(
                onDismissRequest = { onSelectRequest(null) },
                title = {
                    Text(
                        text = "${selectedRequest.method} ${selectedRequest.statusCode}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        Text(
                            text = "URL: ${selectedRequest.url}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Request Headers / Body:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = selectedRequest.requestBody.ifBlank { "[No Request Body]" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Response Preview:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = selectedRequest.responseBody.ifBlank { "[Empty Response]" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onSelectRequest(null) }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
