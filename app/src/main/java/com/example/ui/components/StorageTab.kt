package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CookieItem
import com.example.data.StorageItem

@Composable
fun StorageTab(
    cookies: List<CookieItem>,
    localStorage: List<StorageItem>,
    sessionStorage: List<StorageItem>,
    onUpdateItem: (storageType: String, key: String, value: String) -> Unit = { _, _, _ -> },
    onDeleteItem: (storageType: String, key: String) -> Unit = { _, _ -> },
    onClearStorage: (storageType: String) -> Unit = {},
    onRefreshStorage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedStorageType by remember { mutableStateOf("LocalStorage") }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top Toolbar with Tabs & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                FilterChip(
                    selected = selectedStorageType == "LocalStorage",
                    onClick = { selectedStorageType = "LocalStorage" },
                    label = { Text("Local (${localStorage.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    shape = CircleShape,
                    modifier = Modifier.height(30.dp)
                )
                FilterChip(
                    selected = selectedStorageType == "SessionStorage",
                    onClick = { selectedStorageType = "SessionStorage" },
                    label = { Text("Session (${sessionStorage.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    shape = CircleShape,
                    modifier = Modifier.height(30.dp)
                )
                FilterChip(
                    selected = selectedStorageType == "Cookies",
                    onClick = { selectedStorageType = "Cookies" },
                    label = { Text("Cookies (${cookies.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    shape = CircleShape,
                    modifier = Modifier.height(30.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Add Button
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Refresh Storage
                IconButton(
                    onClick = {
                        onRefreshStorage()
                        Toast.makeText(context, "Storage refreshed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear Storage
                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear All",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Storage Content Items List (Frameless, clean edge-to-edge padding)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedStorageType) {
                "Cookies" -> {
                    if (cookies.isEmpty()) {
                        EmptyStorageState(label = "No Cookies stored for this domain.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize().testTag("cookies_list")
                        ) {
                            items(cookies, key = { it.name }) { cookie ->
                                StorageEntryCard(
                                    key = cookie.name,
                                    value = cookie.value,
                                    tagColor = MaterialTheme.colorScheme.primary,
                                    onCopyKey = {
                                        clipboardManager.setText(AnnotatedString(cookie.name))
                                        Toast.makeText(context, "Cookie name copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onCopyValue = {
                                        clipboardManager.setText(AnnotatedString(cookie.value))
                                        Toast.makeText(context, "Cookie value copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onCopyPair = {
                                        clipboardManager.setText(AnnotatedString("${cookie.name}=${cookie.value}"))
                                        Toast.makeText(context, "Cookie pair copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onEdit = {
                                        editingItem = cookie.name to cookie.value
                                    },
                                    onDelete = {
                                        onDeleteItem("Cookies", cookie.name)
                                        Toast.makeText(context, "Deleted cookie '${cookie.name}'", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
                "LocalStorage" -> {
                    if (localStorage.isEmpty()) {
                        EmptyStorageState(label = "LocalStorage is empty.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize().testTag("local_storage_list")
                        ) {
                            items(localStorage, key = { it.key }) { item ->
                                StorageEntryCard(
                                    key = item.key,
                                    value = item.value,
                                    tagColor = MaterialTheme.colorScheme.secondary,
                                    onCopyKey = {
                                        clipboardManager.setText(AnnotatedString(item.key))
                                        Toast.makeText(context, "Key copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onCopyValue = {
                                        clipboardManager.setText(AnnotatedString(item.value))
                                        Toast.makeText(context, "Value copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onCopyPair = {
                                        clipboardManager.setText(AnnotatedString("${item.key}=${item.value}"))
                                        Toast.makeText(context, "Entry copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onEdit = {
                                        editingItem = item.key to item.value
                                    },
                                    onDelete = {
                                        onDeleteItem("LocalStorage", item.key)
                                        Toast.makeText(context, "Deleted '${item.key}'", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
                "SessionStorage" -> {
                    if (sessionStorage.isEmpty()) {
                        EmptyStorageState(label = "SessionStorage is empty.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize().testTag("session_storage_list")
                        ) {
                            items(sessionStorage, key = { it.key }) { item ->
                                StorageEntryCard(
                                    key = item.key,
                                    value = item.value,
                                    tagColor = MaterialTheme.colorScheme.tertiary,
                                    onCopyKey = {
                                        clipboardManager.setText(AnnotatedString(item.key))
                                        Toast.makeText(context, "Key copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onCopyValue = {
                                        clipboardManager.setText(AnnotatedString(item.value))
                                        Toast.makeText(context, "Value copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onCopyPair = {
                                        clipboardManager.setText(AnnotatedString("${item.key}=${item.value}"))
                                        Toast.makeText(context, "Entry copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onEdit = {
                                        editingItem = item.key to item.value
                                    },
                                    onDelete = {
                                        onDeleteItem("SessionStorage", item.key)
                                        Toast.makeText(context, "Deleted '${item.key}'", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Storage Item Dialog
    if (showAddDialog) {
        StorageEditDialog(
            title = "Add to $selectedStorageType",
            initialKey = "",
            initialValue = "",
            onConfirm = { key, value ->
                if (key.isNotBlank()) {
                    onUpdateItem(selectedStorageType, key, value)
                    Toast.makeText(context, "Added to $selectedStorageType", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit Storage Item Dialog
    editingItem?.let { (key, value) ->
        StorageEditDialog(
            title = "Edit $selectedStorageType Item",
            initialKey = key,
            initialValue = value,
            isKeyReadOnly = true,
            onConfirm = { updatedKey, updatedValue ->
                onUpdateItem(selectedStorageType, updatedKey, updatedValue)
                Toast.makeText(context, "Updated '$updatedKey'", Toast.LENGTH_SHORT).show()
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    // Clear Storage Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear $selectedStorageType?") },
            text = { Text("Are you sure you want to delete all entries in $selectedStorageType?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearStorage(selectedStorageType)
                        Toast.makeText(context, "Cleared $selectedStorageType", Toast.LENGTH_SHORT).show()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageEntryCard(
    key: String,
    value: String,
    tagColor: Color,
    onCopyKey: () -> Unit,
    onCopyValue: () -> Unit,
    onCopyPair: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Key / Name Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = key,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = tagColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = tagColor.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${value.length} chars",
                        fontSize = 10.sp,
                        color = tagColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Value Display Box
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (value.isBlank()) "[Empty Value]" else value,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Action Buttons Row UNDER the element (Copy Key, Copy Value, Copy Pair, Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Copy Key
                    OutlinedButton(
                        onClick = onCopyKey,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Key", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Copy Value
                    OutlinedButton(
                        onClick = onCopyValue,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Value", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Copy Pair
                    OutlinedButton(
                        onClick = onCopyPair,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Edit
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Item", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStorageState(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StorageEditDialog(
    title: String,
    initialKey: String,
    initialValue: String,
    isKeyReadOnly: Boolean = false,
    onConfirm: (key: String, value: String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyText by remember { mutableStateOf(initialKey) }
    var valueText by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { if (!isKeyReadOnly) keyText = it },
                    label = { Text("Key / Name") },
                    readOnly = isKeyReadOnly,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("Value") },
                    maxLines = 5,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(keyText, valueText) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
