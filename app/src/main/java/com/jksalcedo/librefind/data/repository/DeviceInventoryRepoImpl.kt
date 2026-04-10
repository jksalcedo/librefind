package com.jksalcedo.librefind.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.data.local.PackageNameHeuristicsDb
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
import java.util.Locale

class DeviceInventoryRepoImpl(
    private val localSource: InventorySource,
    private val signatureDb: PackageNameHeuristicsDb,
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

        private val OEM_BRANDS = setOf(
            "xiaomi", "redmi", "poco",
            "samsung",
            "oppo", "oneplus", "realme",
            "vivo", "iqoo",
            "huawei", "honor",
            "lenovo", "motorola",
            "meizu", "zte", "nubia"
        )

        private fun isLikelyRomNamespace(packageName: String, prefixes: List<String>): Boolean {
            val p = packageName.lowercase(Locale.US)
            return prefixes.any { prefix -> p.startsWith(prefix) }
        }
    }

    override suspend fun scanAndClassify(): Flow<List<AppItem>> = flow {
        val rawApps = localSource.getRawApps()
        val ignoredAppsList = ignoredAppsRepository.getIgnoredPackageNames().first()
        val reclassifiedAppsMap = reclassifiedAppsRepository.getReclassifiedApps().first()

        val platformSigners = trustedRomSignerDb.platformSigners.first()
        val romAppSigners = trustedRomSignerDb.romAppSigners.first()
        val romPrefixes = trustedRomSignerDb.romPrefixes.first()

        val cacheFresh = cacheRepository.isCacheValid()
//        var usingStaleCache = false

        if (!cacheFresh) {
            try {
                cacheRepository.refreshCache()
            } catch (e: Exception) {
                val hasCache = cacheRepository.hasAnyCache()
                if (hasCache) {
//                    usingStaleCache = true
                    Log.w(TAG, "Offline/stale mode: using existing cache", e)
                } else {
                    Log.w(TAG, "No cache available; continuing with limited classification", e)
                }
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
                        reclassifiedApps = reclassifiedAppsMap,
                        proprietaryMap = proprietaryMap,
                        solutionsSet = solutionsSet,
                        pendingPackages = pendingPackages,
                        platformSigners = platformSigners,
                        romAppSigners = romAppSigners,
                        romPrefixes = romPrefixes
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
        reclassifiedApps: Map<String, AppStatus>,
        proprietaryMap: Map<String, Boolean>,
        solutionsSet: Set<String>,
        pendingPackages: Set<String>,
        platformSigners: Set<String>,
        romAppSigners: Set<String>,
        romPrefixes: List<String>
    ): AppItem {
        val packageName = pkg.packageName
        val label = localSource.getAppLabel(packageName)
        val installer = localSource.getInstaller(packageName)
        val icon = pkg.applicationInfo?.icon

        // Use standard PackageManager flags to determine if it is a system app
        val flags = pkg.applicationInfo?.flags ?: 0
        val isSystem =
            (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

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
            val status = reclassifiedApps[packageName] ?: AppStatus.FOSS
            return createAppItem(
                packageName,
                label,
                status,
                installer,
                icon,
                isUserReclassified = true,
                isSystemPackage = isSystem
            )
        }

        val isAospName = signatureDb.isAospSystemPackageName(packageName)
        if (isAospName) {
            // Non-system app pretending to be com.android.* > suspicious
            if (!isSystem) {
                return createAppItem(
                    packageName, label, AppStatus.PROP, installer, icon,
                    isUserReclassified = false, isSystemPackage = isSystem
                )
            }

            val digests = SignerUtils.signerSha256Digests(pkg)
            val trusted = digests.any { it.lowercase() in platformSigners }

            if (trusted) {
                return createAppItem(
                    packageName, label, AppStatus.FOSS, installer, icon,
                    isUserReclassified = false, isSystemPackage = isSystem
                )
            }

            // Fallback when signer DB is incomplete
            // OEM ROM + AOSP-name system app > likely proprietary fork
            //  otherwise > pending for review
            val brand = Build.BRAND.orEmpty().lowercase(Locale.US)
            val manufacturer = Build.MANUFACTURER.orEmpty().lowercase(Locale.US)
            val fingerprint = Build.FINGERPRINT.orEmpty().lowercase(Locale.US)

            val isLikelyOemRom = OEM_BRANDS.any { oem ->
                brand.contains(oem) || manufacturer.contains(oem) || fingerprint.contains(oem)
            }

            val status = if (isLikelyOemRom) AppStatus.PROP else AppStatus.PENDING

            return createAppItem(
                packageName, label, status, installer, icon,
                isUserReclassified = false, isSystemPackage = isSystem
            )
        }

        if (isSystem && isLikelyRomNamespace(packageName, romPrefixes)) {
            val digests = SignerUtils.signerSha256Digests(pkg)
            if (digests.any { it.lowercase() in romAppSigners }) {
                return createAppItem(
                    packageName, label, AppStatus.FOSS, installer, icon,
                    isUserReclassified = false, isSystemPackage = true
                )
            }
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
