package com.example.ui.components

import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserAgentType

@Composable
fun DevicesSettingsTab(
    currentUserAgent: UserAgentType,
    jsEnabled: Boolean,
    cacheEnabled: Boolean,
    onSelectUserAgent: (UserAgentType) -> Unit,
    onSetJsEnabled: (Boolean) -> Unit,
    onSetCacheEnabled: (Boolean) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Device Emulation & User Agent", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        UserAgentType.entries.forEach { agentType ->
            Surface(
                onClick = { onSelectUserAgent(agentType) },
                shape = RoundedCornerShape(8.dp),
                color = if (currentUserAgent == agentType) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentUserAgent == agentType,
                        onClick = { onSelectUserAgent(agentType) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = agentType.label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = agentType.userAgentString,
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text("Developer Browser Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // JS Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Enable JavaScript", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Allow JS execution on web pages", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = jsEnabled,
                onCheckedChange = onSetJsEnabled,
                modifier = Modifier.testTag("switch_js_enabled")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cache Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Network Cache", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Store web resources in local cache", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = cacheEnabled,
                onCheckedChange = onSetCacheEnabled,
                modifier = Modifier.testTag("switch_cache_enabled")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clear Cookies Button
        Button(
            onClick = {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                Toast.makeText(context, "All Cookies & Storage Cleared", Toast.LENGTH_SHORT).show()
                onReload()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_clear_cookies")
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear All Session Cookies & Cache")
        }
    }
}
