package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConsoleLog
import com.example.data.ConsoleLogLevel
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleTab(
    logs: List<ConsoleLog>,
    selectedLevel: ConsoleLogLevel?,
    searchQuery: String,
    onSelectLevel: (ConsoleLogLevel?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onExecuteJs: (String) -> Unit,
    onClearConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    var jsInputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    val filteredLogs = remember(logs, selectedLevel, searchQuery) {
        logs.filter { log ->
            (selectedLevel == null || log.level == selectedLevel) &&
                    (searchQuery.isBlank() || log.message.contains(searchQuery, ignoreCase = true))
        }
    }

    // Auto scroll to bottom when new logs arrive
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Console Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Filter console...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                shape = RoundedCornerShape(18.dp)
            )

            // Clear Button
            IconButton(
                onClick = onClearConsole,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear Console",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Level Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = selectedLevel == null,
                onClick = { onSelectLevel(null) },
                label = { Text("All (${logs.size})", fontSize = 11.sp) },
                modifier = Modifier.height(28.dp)
            )
            FilterChip(
                selected = selectedLevel == ConsoleLogLevel.ERROR,
                onClick = { onSelectLevel(if (selectedLevel == ConsoleLogLevel.ERROR) null else ConsoleLogLevel.ERROR) },
                label = { Text("Errors (${logs.count { it.level == ConsoleLogLevel.ERROR }})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusError.copy(alpha = 0.2f),
                    selectedLabelColor = StatusError
                ),
                modifier = Modifier.height(28.dp)
            )
            FilterChip(
                selected = selectedLevel == ConsoleLogLevel.WARN,
                onClick = { onSelectLevel(if (selectedLevel == ConsoleLogLevel.WARN) null else ConsoleLogLevel.WARN) },
                label = { Text("Warnings (${logs.count { it.level == ConsoleLogLevel.WARN }})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusWarning.copy(alpha = 0.2f),
                    selectedLabelColor = StatusWarning
                ),
                modifier = Modifier.height(28.dp)
            )
            FilterChip(
                selected = selectedLevel == ConsoleLogLevel.INFO,
                onClick = { onSelectLevel(if (selectedLevel == ConsoleLogLevel.INFO) null else ConsoleLogLevel.INFO) },
                label = { Text("Info (${logs.count { it.level == ConsoleLogLevel.INFO }})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusInfo.copy(alpha = 0.2f),
                    selectedLabelColor = StatusInfo
                ),
                modifier = Modifier.height(28.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

        // Log Output List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("console_logs_list")
        ) {
            items(filteredLogs, key = { it.id }) { log ->
                val levelColor = when (log.level) {
                    ConsoleLogLevel.ERROR -> StatusError
                    ConsoleLogLevel.WARN -> StatusWarning
                    ConsoleLogLevel.INFO -> StatusInfo
                    ConsoleLogLevel.LOG -> MaterialTheme.colorScheme.onSurface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Time Badge
                    Text(
                        text = timeFormatter.format(Date(log.timestamp)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    // Level Badge
                    Surface(
                        color = levelColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = log.level.name,
                            color = levelColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Message
                    Text(
                        text = log.message,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = levelColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Live JS REPL Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ">",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                OutlinedTextField(
                    value = jsInputText,
                    onValueChange = { jsInputText = it },
                    placeholder = { Text("Run JS e.g. document.title or alert()", fontSize = 11.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("console_js_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (jsInputText.isNotBlank()) {
                            onExecuteJs(jsInputText)
                            jsInputText = ""
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (jsInputText.isNotBlank()) {
                            onExecuteJs(jsInputText)
                            jsInputText = ""
                        }
                    },
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .testTag("btn_run_js")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Execute JavaScript",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
