package com.example.ui.components

import android.webkit.CookieManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CookieItem
import com.example.data.StorageItem

@Composable
fun StorageTab(
    cookies: List<CookieItem>,
    localStorage: List<StorageItem>,
    sessionStorage: List<StorageItem>,
    modifier: Modifier = Modifier
) {
    var selectedStorageType by remember { mutableStateOf("Cookies") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Storage Sub-Tabs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedStorageType == "Cookies",
                onClick = { selectedStorageType = "Cookies" },
                label = { Text("Cookies (${cookies.size})", fontSize = 11.sp) },
                modifier = Modifier.height(28.dp)
            )
            FilterChip(
                selected = selectedStorageType == "LocalStorage",
                onClick = { selectedStorageType = "LocalStorage" },
                label = { Text("LocalStorage (${localStorage.size})", fontSize = 11.sp) },
                modifier = Modifier.height(28.dp)
            )
            FilterChip(
                selected = selectedStorageType == "SessionStorage",
                onClick = { selectedStorageType = "SessionStorage" },
                label = { Text("SessionStorage (${sessionStorage.size})", fontSize = 11.sp) },
                modifier = Modifier.height(28.dp)
            )
        }

        HorizontalDivider()

        when (selectedStorageType) {
            "Cookies" -> {
                if (cookies.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Cookies stored for this page.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp).testTag("cookies_list")) {
                        items(cookies) { cookie ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(text = cookie.name, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(text = cookie.value, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
            "LocalStorage" -> {
                if (localStorage.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("LocalStorage is empty.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp).testTag("local_storage_list")) {
                        items(localStorage) { item ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(text = item.key, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                Text(text = item.value, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
            "SessionStorage" -> {
                if (sessionStorage.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("SessionStorage is empty.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp).testTag("session_storage_list")) {
                        items(sessionStorage) { item ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(text = item.key, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                                Text(text = item.value, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}
