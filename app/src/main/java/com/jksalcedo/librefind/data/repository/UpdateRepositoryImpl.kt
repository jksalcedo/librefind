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
import com.jksalcedo.librefind.util.VersionUtils
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
                isUpdateAvailable = VersionUtils.isNewerVersion(latestVersion, currentVersion)
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
}
