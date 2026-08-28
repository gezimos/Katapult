package com.gezimos.katapult.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Obtainium-style in-app updater. Checks GitHub Releases for a newer build,
 * downloads the APK, and hands it to the system package installer.
 *
 * No networking library: raw HttpURLConnection on Dispatchers.IO, matching the
 * app's zero-dependency style. Manual check only (no background polling).
 */
object UpdateChecker {

    private const val LATEST_URL =
        "https://api.github.com/repos/gezimos/Katapult/releases/latest"
    private const val APK_FILE = "update.apk"

    data class ReleaseInfo(
        val versionName: String, // tag without a leading "v", e.g. "1.4"
        val tag: String,         // raw tag, e.g. "v1.4"
        val notes: String,       // release body / changelog
        val apkUrl: String,      // browser_download_url of the .apk asset
    )

    /** GET the latest release; null on any failure (offline, rate-limited, no APK asset). */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                // GitHub rejects requests without a User-Agent.
                setRequestProperty("User-Agent", "Katapult")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val body = conn.use {
                if (it.responseCode != 200) return@withContext null
                it.inputStream.bufferedReader().readText()
            }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").ifBlank { return@withContext null }
            val notes = json.optString("body")
            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isNullOrBlank()) return@withContext null
            ReleaseInfo(
                versionName = tag.removePrefix("v").removePrefix("V"),
                tag = tag,
                notes = notes,
                apkUrl = apkUrl,
            )
        } catch (_: Exception) {
            null
        }
    }

    /** True if [latestTag] represents a version strictly newer than [currentVersionName]. */
    fun isNewer(latestTag: String, currentVersionName: String): Boolean {
        val latest = parseVersion(latestTag)
        val current = parseVersion(currentVersionName)
        val n = maxOf(latest.size, current.size)
        for (i in 0 until n) {
            val a = latest.getOrElse(i) { 0 }
            val b = current.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parseVersion(v: String): List<Int> =
        v.removePrefix("v").removePrefix("V")
            .split(".")
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    /** The file the APK is downloaded to (overwritten each time). */
    fun apkFile(context: Context): File = File(context.cacheDir, APK_FILE)

    /**
     * Stream the APK to [dest], reporting 0-100 progress. Returns true on success.
     * Progress is only meaningful when the server sends Content-Length.
     */
    suspend fun downloadApk(
        url: String,
        dest: File,
        onProgress: (Int) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Katapult")
            }
            conn.use {
                if (it.responseCode != 200) return@withContext false
                val total = it.contentLength.toLong()
                var read = 0L
                it.inputStream.use { input ->
                    dest.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            read += count
                            if (total > 0) onProgress(((read * 100) / total).toInt())
                        }
                    }
                }
                if (total > 0 && read != total) {
                    dest.delete()
                    return@withContext false
                }
            }
            true
        } catch (_: Exception) {
            dest.delete()
            false
        }
    }

    /**
     * Launch the system installer for [file]. If unknown-sources install isn't
     * permitted yet, send the user to that settings screen first and return false.
     */
    fun installApk(context: Context, file: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:" + context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {}
            return false
        }
        return try {
            val uri = FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider", file,
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    /** HttpURLConnection has no Closeable disconnect helper; provide one. */
    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T {
        try {
            return block(this)
        } finally {
            disconnect()
        }
    }
}
