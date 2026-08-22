package com.example.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val apkUrl: String,
    val publishedAt: String,
    val isNewer: Boolean
)

object AppUpdateManager {

    suspend fun checkForUpdate(repo: String, currentVersion: String): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val cleanRepo = repo.trim().removePrefix("https://github.com/").trim('/')
            if (cleanRepo.isBlank() || !cleanRepo.contains("/")) return@withContext null

            val urlString = "https://api.github.com/repos/$cleanRepo/releases/latest"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "DevBrowser-AppUpdate")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != 200) {
                // Fallback to all releases list if /latest is not found
                return@withContext checkReleasesList(cleanRepo, currentVersion)
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            return@withContext parseReleaseJson(json, currentVersion)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun checkReleasesList(repo: String, currentVersion: String): GitHubRelease? {
        return try {
            val url = URL("https://api.github.com/repos/$repo/releases")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "DevBrowser-AppUpdate")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == 200) {
                val arrayStr = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(arrayStr)
                if (arr.length() > 0) {
                    parseReleaseJson(arr.getJSONObject(0), currentVersion)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseReleaseJson(json: JSONObject, currentVersion: String): GitHubRelease? {
        val tagName = json.optString("tag_name", "").trim()
        val name = json.optString("name", tagName).ifBlank { tagName }
        val body = json.optString("body", "No release notes provided.")
        val publishedAt = json.optString("published_at", "")

        // Find APK asset URL
        var apkUrl = ""
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "")
                    if (assetName.contains("release", ignoreCase = true)) break // Prefer release APK if found
                }
            }
        }

        if (tagName.isBlank() && apkUrl.isBlank()) return null

        val cleanTag = tagName.removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.removePrefix("v").removePrefix("V")
        val isNewer = isVersionNewer(cleanTag, cleanCurrent)

        return GitHubRelease(
            tagName = tagName,
            name = name,
            body = body,
            apkUrl = apkUrl,
            publishedAt = publishedAt,
            isNewer = isNewer
        )
    }

    private fun isVersionNewer(remoteVer: String, currentVer: String): Boolean {
        if (remoteVer == currentVer) return false
        val remoteParts = remoteVer.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentVer.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return remoteVer != currentVer
    }

    suspend fun downloadApkWithProgress(
        context: Context,
        downloadUrl: String,
        tagName: String,
        onProgress: suspend (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        if (downloadUrl.isBlank()) return@withContext null

        try {
            val fileName = "DevBrowser_$tagName.apk"
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destinationFile.exists()) destinationFile.delete()

            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirects = 0

            while (redirects < 5) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "DevBrowser-AppUpdate")
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    if (!newUrl.isNullOrBlank()) {
                        currentUrl = newUrl
                        redirects++
                        continue
                    }
                }

                if (status != HttpURLConnection.HTTP_OK) {
                    return@withContext null
                }

                val totalLength = connection.contentLengthLong
                connection.inputStream.use { input ->
                    destinationFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        var lastReportedTime = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastReportedTime > 150 || totalRead == totalLength) {
                                lastReportedTime = now
                                val percent = if (totalLength > 0) ((totalRead * 100) / totalLength).toInt() else -1
                                onProgress(percent, totalRead, totalLength)
                            }
                        }
                    }
                }
                return@withContext destinationFile
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun downloadAndInstallApk(context: Context, downloadUrl: String, tagName: String) {
        if (downloadUrl.isBlank()) return

        val fileName = "DevBrowser_$tagName.apk"
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Downloading App Update ($tagName)")
            setDescription("Downloading APK from GitHub releases...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(destinationFile))
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id == downloadId) {
                    try { ctxt?.unregisterReceiver(this) } catch (_: Exception) {}
                    installApk(context, destinationFile)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
