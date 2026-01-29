package com.jksalcedo.librefind.data.repository

import android.content.pm.PackageInfo
import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.data.local.SafeSignatureDb
import com.jksalcedo.librefind.domain.model.AppItem
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.DeviceInventoryRepo
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Implementation of DeviceInventoryRepo
 *
 * Implements the three-step classification logic:
 * A. Source Check (Fast Filter)
 * B. Signature Check (Verification)
 * C. Database Query (LibreFind Cloud)
 */
class DeviceInventoryRepoImpl(
    private val localSource: InventorySource,
    private val signatureDb: SafeSignatureDb,
    private val appRepository: AppRepository,
    private val ignoredAppsRepository: IgnoredAppsRepository
) : DeviceInventoryRepo {

    companion object {
        private const val FDROID_INSTALLER = "org.fdroid.fdroid"
        private const val PLAY_STORE_INSTALLER = "com.android.vending"
    }

    override suspend fun scanAndClassify(): Flow<List<AppItem>> = flow {
        val rawApps = localSource.getRawApps()
        val ignoredAppsList = ignoredAppsRepository.getIgnoredPackageNames().first()

        val classifiedApps = coroutineScope {

            rawApps.map { pkg ->
                async { classifyApp(pkg, ignoredAppsList) }
            }.awaitAll()
        }

        // Sort by classification priority (PROP > UNKN > FOSS)
        val sorted = classifiedApps.sortedBy { it.status.sortWeight }

        emit(sorted)
    }.flowOn(Dispatchers.IO)

    /**
     * Classifies a single app using the three-step logic
     */
    private suspend fun classifyApp(pkg: PackageInfo, ignoredApps: List<String>): AppItem {
        val packageName = pkg.packageName
        val label = localSource.getAppLabel(packageName)
        val installer = localSource.getInstaller(packageName)
        val icon = pkg.applicationInfo?.icon

        if (packageName in ignoredApps) {
            return createAppItem(
                packageName,
                label,
                AppStatus.IGNORED,
                installer,
                icon
            )
        }

        // Fast Filter - Check installer
        if (installer == FDROID_INSTALLER) {
            return createAppItem(packageName, label, AppStatus.FOSS, installer, icon)
        }

        // Signature Check (for FOSS apps on Play Store)
        if (signatureDb.isKnownFossApp(packageName)) {
            return createAppItem(packageName, label, AppStatus.FOSS, installer, icon)
        }

        // Database Query (Solutions)
        // Check if it's a known FOSS solution in our database
        val isKnownSolution = try {
            appRepository.isSolution(packageName)
        } catch (_: Exception) {
            false
        }

        if (isKnownSolution) {
            return createAppItem(packageName, label, AppStatus.FOSS, installer, icon)
        }

        // Database Query
        val isProprietary = try {
            appRepository.isProprietary(packageName)
        } catch (_: Exception) {
            false
        }

        val status = if (isProprietary) AppStatus.PROP else AppStatus.UNKN

        return createAppItem(packageName, label, status, installer, icon)
    }

    /**
     * Creates AppItem with alternatives count
     */
    private suspend fun createAppItem(
        packageName: String,
        label: String,
        status: AppStatus,
        installer: String?,
        icon: Int?
    ): AppItem {
        val alternativesCount = if (status == AppStatus.PROP) {
            try {
                appRepository.getAlternatives(packageName).size
            } catch (_: Exception) {
                0
            }
        } else {
            0
        }

        return AppItem(
            packageName = packageName,
            label = label,
            status = status,
            installerId = installer,
            knownAlternatives = alternativesCount,
            icon = icon
        )
    }

    override fun getInstaller(packageName: String): String? {
        return localSource.getInstaller(packageName)
    }
}
