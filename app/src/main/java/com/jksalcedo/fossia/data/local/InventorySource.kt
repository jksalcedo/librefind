package com.jksalcedo.fossia.data.local

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for device app inventory
 * 
 * Wraps Android PackageManager API to extract installed packages.
 * This is the "Eyes" of the detection system.
 */
@Singleton
class InventorySource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Gets all user-installed apps
     * 
     * Filters out pure system apps, but keeps:
     * - User-installed apps
     * - Updated system apps (e.g., Gmail, Maps)
     * 
     * @return List of PackageInfo for user apps
     */
    fun getRawApps(): List<PackageInfo> {
        return try {
            context.packageManager
                .getInstalledPackages(PackageManager.GET_META_DATA)
                .filter { app ->
                    // Filter logic: User apps + Updated System Apps only
                    (app.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) ||
                    (app.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets the installer package name for a specific app
     * 
     * Handles API level differences:
     * - Android R (API 30+): Uses getInstallSourceInfo
     * - Pre-R: Uses deprecated getInstallerPackageName
     * 
     * @param packageName Package to query
     * @return Installer package name or null if unknown/error
     */
    fun getInstaller(packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager
                    .getInstallSourceInfo(packageName)
                    .installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets human-readable label for an app
     * 
     * @param packageName Package to query
     * @return App label or package name if label unavailable
     */
    fun getAppLabel(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
