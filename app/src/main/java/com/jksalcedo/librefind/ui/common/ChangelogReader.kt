package com.jksalcedo.librefind.ui.common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object ChangelogReader {

    fun readChangelog(context: Context): Pair<String, String> {
        val versionCode = getVersionCode(context)
        val versionName = getVersionName(context)

        val changelog = try {
            context.assets.open("changelogs/$versionCode.txt").bufferedReader()
                .use { it.readText() }
        } catch (_: Exception) {
            "No changelog available for this version."
        }

        return Pair(versionName, changelog)
    }

    private fun getVersionCode(context: Context): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionCode
        } catch (_: Exception) {
            0
        }
    }

    private fun getVersionName(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
