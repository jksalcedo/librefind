package com.jksalcedo.librefind.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.util.Log
import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.data.local.KnownFossPackages
import com.jksalcedo.librefind.data.local.TrustedRomSignerDb
import com.jksalcedo.librefind.domain.model.AppItem
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.CacheRepository
import com.jksalcedo.librefind.domain.repository.DeviceInventoryRepo
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import com.jksalcedo.librefind.domain.repository.ReclassifiedAppsRepository
import com.jksalcedo.librefind.util.SignerUtils
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
    private val signatureDb: KnownFossPackages,
    private val appRepository: AppRepository,
    private val ignoredAppsRepository: IgnoredAppsRepository,
    private val cacheRepository: CacheRepository,
    private val reclassifiedAppsRepository: ReclassifiedAppsRepository,
    private val trustedRomSignerDb: TrustedRomSignerDb
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
            "app.flicky",
            "dev.imranr.obtainium.fdroid"
        )

        // Apps installed FROM these are proprietary
        private val PROPRIETARY_INSTALLERS = setOf(
            "com.android.vending",
            "com.aurora.store",
            "com.apkpure.aegon",
            "com.tomclaw.appsend",
            "com.indus.appstore",
            "com.apkupdater"
        )
    }

    override suspend fun scanAndClassify(): Flow<List<AppItem>> = flow {
        val rawApps = localSource.getRawApps()
        val ignoredAppsList = ignoredAppsRepository.getIgnoredPackageNames().first()
        val reclassifiedAppsList = reclassifiedAppsRepository.getReclassifiedPackageNames().first()

        if (!cacheRepository.isCacheValid()) {
            try {
                cacheRepository.refreshCache()
            } catch (e: Exception) {
                Log.w(TAG, "Cache refresh failed, using remote fallback", e)
            }
        }

        val packageNames = rawApps.map { it.packageName }

        // Bulk lookups
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
                        reclassifiedApps = reclassifiedAppsList,
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
        reclassifiedApps: List<String>,
        proprietaryMap: Map<String, Boolean>,
        solutionsSet: Set<String>,
        pendingPackages: Set<String>
    ): AppItem {
        val packageName = pkg.packageName
        val label = localSource.getAppLabel(packageName)
        val installer = localSource.getInstaller(packageName)
        val icon = pkg.applicationInfo?.icon

        // Use standard PackageManager flags to determine if it is a system app
        val isSystem = (pkg.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0

        if (packageName in ignoredApps) {
            return createAppItem(
                packageName,
                label,
                AppStatus.IGNORED,
                installer,
                icon,
                isUserReclassified = false,
                isSystemPackage = isSystem
            )
        }

        if (packageName in reclassifiedApps) {
            return createAppItem(
                packageName,
                label,
                AppStatus.FOSS,
                installer,
                icon,
                isUserReclassified = true,
                isSystemPackage = isSystem
            )
        }

        val isAospName = signatureDb.isAospSystemPackageName(packageName)
        if (isAospName) {
            val digests = SignerUtils.signerSha256Digests(pkg)
            val trusted = trustedRomSignerDb.isTrustedSigner(digests)

            if (!trusted) {
                // Force PROP, even if installed from F-Droid, etc.
                return createAppItem(
                    packageName,
                    label,
                    AppStatus.PROP,
                    installer,
                    icon,
                    isUserReclassified = false,
                    isSystemPackage = isSystem
                )
            }

            return createAppItem(
                packageName,
                label,
                AppStatus.FOSS,
                installer,
                icon,
                isUserReclassified = false,
                isSystemPackage = isSystem
            )
        }

        if (installer in FOSS_INSTALLERS) {
            return createAppItem(
                packageName,
                label,
                AppStatus.FOSS,
                installer,
                icon,
                isUserReclassified = false,
                isSystemPackage = isSystem
            )
        }

        if (signatureDb.isKnownFossApp(packageName)) {
            return createAppItem(
                packageName,
                label,
                AppStatus.FOSS,
                installer,
                icon,
                isUserReclassified = false,
                isSystemPackage = isSystem
            )
        }

        if (signatureDb.isAospSystemPackageName(packageName)) {
            //only apply this rule to system apps
            if (isSystem) {
                val digests = SignerUtils.signerSha256Digests(pkg)
                val trusted = trustedRomSignerDb.isTrustedSigner(digests)

                return createAppItem(
                    packageName,
                    label,
                    if (trusted) AppStatus.FOSS else AppStatus.PROP,
                    installer,
                    icon,
                    isUserReclassified = false,
                    isSystemPackage = isSystem
                )
            }
            // If it's not a system app but uses com.android.* package name, treat as PROP
            return createAppItem(
                packageName, label, AppStatus.PROP, installer, icon,
                isUserReclassified = false, isSystemPackage = isSystem
            )
        }

        val isKnownSolution = try {
            cacheRepository.isSolutionCached(packageName) || packageName in solutionsSet
        } catch (_: Exception) {
            false
        }

        if (isKnownSolution) {
            return createAppItem(
                packageName,
                label,
                AppStatus.FOSS,
                installer,
                icon,
                isUserReclassified = false,
                isSystemPackage = isSystem
            )
        }

        val isProprietary = try {
            cacheRepository.isTargetCached(packageName) || (proprietaryMap[packageName] == true)
        } catch (_: Exception) {
            false
        }

        if (isProprietary) {
            return createAppItem(
                packageName,
                label,
                AppStatus.PROP,
                installer,
                icon,
                isUserReclassified = false,
                isSystemPackage = isSystem
            )
        }

        if (installer in PROPRIETARY_INSTALLERS) {
            return createAppItem(
                packageName,
                label,
                AppStatus.PROP,
                installer,
                icon,
                isUserReclassified = false,
                isSystemPackage = isSystem
            )
        }

        // Only show PENDING if app isn't already classified
        if (packageName in pendingPackages) {
            return createAppItem(
                packageName,
                label,
                AppStatus.PENDING,
                installer,
                icon,
                isUserReclassified = false,
                isSystemPackage = isSystem
            )
        }

        return createAppItem(
            packageName,
            label,
            AppStatus.UNKN,
            installer,
            icon,
            isUserReclassified = false,
            isSystemPackage = isSystem
        )
    }

    private suspend fun createAppItem(
        packageName: String,
        label: String,
        status: AppStatus,
        installer: String?,
        icon: Int?,
        isUserReclassified: Boolean = false,
        isSystemPackage: Boolean = false
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
            icon = icon,
            isUserReclassified = isUserReclassified,
            isSystemPackage = isSystemPackage
        )
    }

    override fun getInstaller(packageName: String): String? {
        return localSource.getInstaller(packageName)
    }
}
