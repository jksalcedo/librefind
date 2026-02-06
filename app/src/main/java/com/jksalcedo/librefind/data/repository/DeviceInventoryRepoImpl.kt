package com.jksalcedo.librefind.data.repository

import android.content.pm.PackageInfo
import android.util.Log
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
        private const val TAG = "DeviceInventory"

        // Apps installed FROM these are FOSS
        private val FOSS_INSTALLERS = setOf(
            "org.fdroid.fdroid",
            "com.machiav3lli.fdroid",
            "com.looker.droidify",
            "nya.kitsunyan.foxydroid",
            "in.sunilpaulmathew.izzyondroid",
            "dev.zapstore.app",
            "app.accrescent.client",
            "com.samyak.repostore",
            "com.nahnah.florid",
            "ie.defo.ech_apps",
            "app.flicky"
        )

        // Apps installed FROM these are proprietary
        private val PROPRIETARY_INSTALLERS = setOf(
            "com.android.vending",
            "com.aurora.store",
            "com.apkpure.aegon",
            "dev.imranr.obtainium.fdroid",
            "com.tomclaw.appsend",
            "com.indus.appstore",
            "com.apkupdater"
        )
    }

    override suspend fun scanAndClassify(): Flow<List<AppItem>> = flow {
        val rawApps = localSource.getRawApps()
        val ignoredAppsList = ignoredAppsRepository.getIgnoredPackageNames().first()

        if (!cacheRepository.isCacheValid()) {
            try {
                cacheRepository.refreshCache()
            } catch (e: Exception) {
                Log.w(TAG, "Cache refresh failed, using remote fallback", e)
            }
        }

        val packageNames = rawApps.map { it.packageName }

        // Bulk lookups — all done upfront, no per-app network calls
        val proprietaryMap = try {
            appRepository.areProprietary(packageNames)
        } catch (_: Exception) {
            emptyMap()
        }

        val solutionsSet = try {
            appRepository.areSolutions(packageNames)
        } catch (_: Exception) {
            emptySet()
        }

        val pendingPackages = try {
            appRepository.getPendingSubmissionPackages()
        } catch (_: Exception) {
            emptySet()
        }

        val classifiedApps = coroutineScope {
            rawApps.map { pkg ->
                async {
                    classifyApp(
                        pkg = pkg,
                        ignoredApps = ignoredAppsList,
                        proprietaryMap = proprietaryMap,
                        solutionsSet = solutionsSet,
                        pendingPackages = pendingPackages
                    )
                }
            }.awaitAll()
        }

        val sorted = classifiedApps.sortedBy { it.status.sortWeight }
        emit(sorted)
    }.flowOn(Dispatchers.IO)

    private suspend fun classifyApp(
        pkg: PackageInfo,
        ignoredApps: List<String>,
        proprietaryMap: Map<String, Boolean>,
        solutionsSet: Set<String>,
        pendingPackages: Set<String>
    ): AppItem {
        val packageName = pkg.packageName
        val label = localSource.getAppLabel(packageName)
        val installer = localSource.getInstaller(packageName)
        val icon = pkg.applicationInfo?.icon

        if (packageName in ignoredApps) {
            return createAppItem(packageName, label, AppStatus.IGNORED, installer, icon)
        }

        if (packageName in pendingPackages) {
            return createAppItem(packageName, label, AppStatus.PENDING, installer, icon)
        }

        if (installer in FOSS_INSTALLERS) {
            return createAppItem(packageName, label, AppStatus.FOSS, installer, icon)
        }

        if (signatureDb.isKnownFossApp(packageName)) {
            return createAppItem(packageName, label, AppStatus.FOSS, installer, icon)
        }

        // Use pre-fetched bulk data — no per-app network calls
        val isKnownSolution = try {
            cacheRepository.isSolutionCached(packageName) || packageName in solutionsSet
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

        if (installer in PROPRIETARY_INSTALLERS) {
            return createAppItem(packageName, label, AppStatus.PROP, installer, icon)
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