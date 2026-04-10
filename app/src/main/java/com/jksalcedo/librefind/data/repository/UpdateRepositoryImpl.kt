package com.jksalcedo.librefind.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.net.toUri
import com.jksalcedo.librefind.data.remote.UpdateApiService
import com.jksalcedo.librefind.domain.model.AppUpdate
import com.jksalcedo.librefind.domain.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateRepositoryImpl(
    private val context: Context,
    private val updateApiService: UpdateApiService
) : UpdateRepository {

    override suspend fun checkForUpdate(): Result<AppUpdate> = withContext(Dispatchers.IO) {
        runCatching {
            val release = updateApiService.getLatestRelease()
            val latestVersion = release.tagName.removePrefix("v")
            val currentVersion = try {
                val pm = context.packageManager
                val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(context.packageName, 0)
                }
                pkg.versionName ?: "unknown"
            } catch (_: Exception) {
                "unknown"
            }

            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                ?: throw Exception("No APK found in the latest release")

            AppUpdate(
                version = latestVersion,
                changelog = release.body,
                downloadUrl = apkAsset.downloadUrl,
                fileName = apkAsset.name,
                isUpdateAvailable = isNewerVersion(latestVersion, currentVersion)
            )
        }
    }

    override fun downloadUpdate(url: String, fileName: String): Long {
        val request = DownloadManager.Request(url.toUri())
            .setTitle("LibreFind Update")
            .setDescription("Downloading latest version of LibreFind")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        // Simple semantic versioning comparison
        val latestParts = latest.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }

        val size = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until size) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }

        // If versions are same, check for suffix (e.g. beta11 vs stable/nothing)
        // For development/simplicity, if they are exactly equal, no update.
        return false
    }
}
