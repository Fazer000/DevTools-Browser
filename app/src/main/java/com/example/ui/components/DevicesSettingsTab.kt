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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GitHubRelease
import com.example.data.UserAgentType

@Composable
fun DevicesSettingsTab(
    currentUserAgent: UserAgentType,
    jsEnabled: Boolean,
    cacheEnabled: Boolean,
    githubRepo: String,
    githubRelease: GitHubRelease?,
    isCheckingUpdate: Boolean,
    updateCheckStatus: String?,
    downloadProgress: Float?,
    downloadStatusText: String?,
    onSelectUserAgent: (UserAgentType) -> Unit,
    onSetJsEnabled: (Boolean) -> Unit,
    onSetCacheEnabled: (Boolean) -> Unit,
    onSetGithubRepo: (String) -> Unit,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
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
        // App Update Section
        Text("In-App Update from GitHub", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text("Check and install latest APK release directly from GitHub repository", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        var repoInput by remember(githubRepo) { mutableStateOf(githubRepo) }

        OutlinedTextField(
            value = repoInput,
            onValueChange = {
                repoInput = it
                onSetGithubRepo(it)
            },
            label = { Text("GitHub Repository (owner/repo)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCheckForUpdate,
                enabled = !isCheckingUpdate && repoInput.isNotBlank(),
                modifier = Modifier.testTag("btn_check_github_update")
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Checking...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check for Updates", fontSize = 12.sp)
                }
            }

            if (githubRelease?.isNewer == true && !githubRelease.apkUrl.isBlank()) {
                Button(
                    onClick = onInstallUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Install APK", fontSize = 12.sp)
                }
            }
        }

        if (!updateCheckStatus.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = updateCheckStatus,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (githubRelease?.isNewer == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (downloadProgress != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!downloadStatusText.isNullOrBlank()) {
                    Text(
                        text = downloadStatusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                LinearProgressIndicator(
                    progress = { downloadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        if (githubRelease != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Release: ${githubRelease.name} (${githubRelease.tagName})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (githubRelease.publishedAt.isNotBlank()) {
                        Text(text = "Published: ${githubRelease.publishedAt}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = githubRelease.body.take(300),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (githubRelease.apkUrl.isBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("⚠️ No APK asset attached to this release tag.", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

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
