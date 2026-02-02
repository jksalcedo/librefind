package com.jksalcedo.librefind.data.repository

import android.content.pm.PackageInfo
import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.data.local.SafeSignatureDb
import com.jksalcedo.librefind.domain.model.AppItem
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.CacheRepository
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

class DeviceInventoryRepoImpl(
    private val localSource: InventorySource,
    private val signatureDb: SafeSignatureDb,
    private val appRepository: AppRepository,
    private val ignoredAppsRepository: IgnoredAppsRepository,
    private val cacheRepository: CacheRepository
) : DeviceInventoryRepo {

    companion object {
        private const val FDROID_INSTALLER = "org.fdroid.fdroid"
        private const val PLAY_STORE_INSTALLER = "com.android.vending"
    }

    override suspend fun scanAndClassify(): Flow<List<AppItem>> = flow {
        val rawApps = localSource.getRawApps()
        val ignoredAppsList = ignoredAppsRepository.getIgnoredPackageNames().first()

        if (!cacheRepository.isCacheValid()) {
            try {
                cacheRepository.refreshCache()
            } catch (e: Exception) {
                android.util.Log.w(
                    "DeviceInventory",
                    "Cache refresh failed, using remote fallback",
                    e
                )
            }
        }

        // bulk proprietary lookup to avoid per-app calls
        val packageNames = rawApps.map { it.packageName }
        val proprietaryMap = try {
            appRepository.areProprietary(packageNames)
        } catch (_: Exception) {
            emptyMap()
        }

        val classifiedApps = coroutineScope {
            rawApps.map { pkg ->
                async { classifyApp(pkg, ignoredAppsList, proprietaryMap) }
            }.awaitAll()
        }

        val sorted = classifiedApps.sortedBy { it.status.sortWeight }
        emit(sorted)
    }.flowOn(Dispatchers.IO)

    // use proprietaryMap
    private suspend fun classifyApp(
        pkg: PackageInfo,
        ignoredApps: List<String>,
        proprietaryMap: Map<String, Boolean>
    ): AppItem {
        val packageName = pkg.packageName
        val label = localSource.getAppLabel(packageName)
        val installer = localSource.getInstaller(packageName)
        val icon = pkg.applicationInfo?.icon

        if (packageName in ignoredApps) {
            return createAppItem(packageName, label, AppStatus.IGNORED, installer, icon)
        }

        if (installer == FDROID_INSTALLER) {
            return createAppItem(packageName, label, AppStatus.FOSS, installer, icon)
        }

        if (signatureDb.isKnownFossApp(packageName)) {
            return createAppItem(packageName, label, AppStatus.FOSS, installer, icon)
        }

        val isKnownSolution = try {
            cacheRepository.isSolutionCached(packageName) || appRepository.isSolution(packageName)
        } catch (_: Exception) {
            false
        }

        if (isKnownSolution) {
            return createAppItem(packageName, label, AppStatus.FOSS, installer, icon)
        }

        val isProprietary = try {
            cacheRepository.isTargetCached(packageName) || (proprietaryMap[packageName] == true)
        } catch (_: Exception) {
            false
        }

        val status = if (isProprietary) AppStatus.PROP else AppStatus.UNKN
        return createAppItem(packageName, label, status, installer, icon)
    }

    private suspend fun createAppItem(
        packageName: String,
        label: String,
        status: AppStatus,
        installer: String?,
        icon: Int?
    ): AppItem {
        val alternativesCount = if (status == AppStatus.PROP) {
            try {
                cacheRepository.getAlternativesCount(packageName)
                    ?: appRepository.getAlternativesCount(packageName)
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
