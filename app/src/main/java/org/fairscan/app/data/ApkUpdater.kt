/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.fairscan.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fairscan.app.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ApkUpdater(private val context: Context) {
    data class Release(val version: String, val downloadUrl: String)

    suspend fun latestRelease(): Release? = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "FairScan-Android-Updater")
        }
        try {
            if (connection.responseCode == 404) return@withContext null
            if (connection.responseCode !in 200..299) error("GitHub returned ${connection.responseCode}")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optBoolean("draft") || json.optBoolean("prerelease")) return@withContext null
            val version = json.getString("tag_name").removePrefix("fork-v").removePrefix("v")
            if (compareVersions(version, BuildConfig.VERSION_NAME) <= 0) return@withContext null
            val assets = json.getJSONArray("assets")
            val supported = Build.SUPPORTED_ABIS.toList()
            val candidates = (0 until assets.length()).map { assets.getJSONObject(it) }
                .filter { it.optLong("size") > 0 && it.optString("name").endsWith(".apk", true) }
            val selected = supported.firstNotNullOfOrNull { abi ->
                candidates.firstOrNull { it.getString("name").contains(abi, ignoreCase = true) }
            } ?: candidates.firstOrNull { it.getString("name").contains("universal", true) }
            selected?.let { Release(version, it.getString("browser_download_url")) }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(release: Release): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(directory, "FairScan-${release.version}.apk")
        val connection = (URL(release.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "FairScan-Android-Updater")
        }
        try {
            if (connection.responseCode !in 200..299) error("APK download failed: ${connection.responseCode}")
            connection.inputStream.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
        } finally {
            connection.disconnect()
        }
        apk
    }

    fun install(apk: File): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val b = right.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val result = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (result != 0) return result
        }
        return 0
    }

    companion object {
        private const val RELEASE_API = "https://api.github.com/repos/fidanci/FairScan/releases/latest"
        private const val APK_MIME = "application/vnd.android.package-archive"
    }
}
