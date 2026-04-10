package com.jksalcedo.librefind.data.repository

import android.content.pm.PackageInfo
import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.data.local.PackageNameHeuristicsDb
import com.jksalcedo.librefind.data.local.TrustedRomSignerDb
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.CacheRepository
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import com.jksalcedo.librefind.domain.repository.ReclassifiedAppsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeviceInventoryRepoImplTest {

    private val localSource: InventorySource = mockk()
    private val signatureDb: PackageNameHeuristicsDb = mockk()
    private val appRepository: AppRepository = mockk()
    private val ignoredAppsRepository: IgnoredAppsRepository = mockk()
    private val cacheRepository: CacheRepository = mockk()
    private val reclassifiedAppsRepository: ReclassifiedAppsRepository = mockk()
    private val trustedRomSignerDb: TrustedRomSignerDb = mockk()

    private lateinit var repo: DeviceInventoryRepoImpl

    @Before
    fun setup() {
        repo = DeviceInventoryRepoImpl(
            localSource, signatureDb, appRepository, ignoredAppsRepository,
            cacheRepository, reclassifiedAppsRepository, trustedRomSignerDb
        )

        // Default mocks
        coEvery { ignoredAppsRepository.getIgnoredPackageNames() } returns flowOf(emptyList())
        coEvery { reclassifiedAppsRepository.getReclassifiedApps() } returns flowOf(emptyMap())
        every { trustedRomSignerDb.platformSigners } returns flowOf(emptySet())
        every { trustedRomSignerDb.romAppSigners } returns flowOf(emptySet())
        every { trustedRomSignerDb.romPrefixes } returns flowOf(emptyList())
        coEvery { cacheRepository.isCacheValid() } returns true
        coEvery { cacheRepository.isSolutionCached(any()) } returns false
        coEvery { cacheRepository.isTargetCached(any()) } returns false
        coEvery { cacheRepository.getAlternativesCount(any()) } returns 0
        coEvery { appRepository.areProprietary(any()) } returns emptyMap()
        coEvery { appRepository.areSolutions(any()) } returns emptySet()
        coEvery { appRepository.getPendingSubmissionPackages() } returns emptySet()
        every { signatureDb.isAospSystemPackageName(any()) } returns false
    }

    @Test
    fun `scanAndClassify classifies FOSS installer correctly`() = runTest {
        // Arrange
        val pkg = mockk<PackageInfo> {
            packageName = "com.test.foss"
            applicationInfo = mockk {
                flags = 0
                icon = 0
            }
        }
        coEvery { localSource.getRawApps() } returns listOf(pkg)
        every { localSource.getAppLabel(any()) } returns "FOSS App"
        every { localSource.getInstaller(any()) } returns "org.fdroid.fdroid"

        // Act
        val result = repo.scanAndClassify().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(AppStatus.FOSS, result[0].status)
    }

    @Test
    fun `scanAndClassify classifies Play Store installer correctly`() = runTest {
        // Arrange
        val pkg = mockk<PackageInfo> {
            packageName = "com.test.prop"
            applicationInfo = mockk {
                flags = 0
                icon = 0
            }
        }
        coEvery { localSource.getRawApps() } returns listOf(pkg)
        every { localSource.getAppLabel(any()) } returns "Prop App"
        every { localSource.getInstaller(any()) } returns "com.android.vending"

        // Act
        val result = repo.scanAndClassify().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(AppStatus.PROP, result[0].status)
    }

    @Test
    fun `scanAndClassify respects reclassification`() = runTest {
        // Arrange
        val pkg = mockk<PackageInfo> {
            packageName = "com.test.reclassified"
            applicationInfo = mockk {
                flags = 0
                icon = 0
            }
        }
        coEvery { localSource.getRawApps() } returns listOf(pkg)
        every { localSource.getAppLabel(any()) } returns "Reclassified App"
        every { localSource.getInstaller(any()) } returns "com.android.vending"
        coEvery { reclassifiedAppsRepository.getReclassifiedApps() } returns flowOf(mapOf("com.test.reclassified" to AppStatus.FOSS))

        // Act
        val result = repo.scanAndClassify().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(AppStatus.FOSS, result[0].status)
        assertEquals(true, result[0].isUserReclassified)
    }

    @Test
    fun `scanAndClassify sorts results by AppStatus`() = runTest {
        // Arrange
        val pkgFoss = mockk<PackageInfo> {
            packageName = "com.test.foss"
            applicationInfo = mockk { flags = 0; icon = 0 }
        }
        val pkgProp = mockk<PackageInfo> {
            packageName = "com.test.prop"
            applicationInfo = mockk { flags = 0; icon = 0 }
        }
        coEvery { localSource.getRawApps() } returns listOf(pkgFoss, pkgProp)
        every { localSource.getAppLabel("com.test.foss") } returns "FOSS"
        every { localSource.getAppLabel("com.test.prop") } returns "PROP"
        every { localSource.getInstaller("com.test.foss") } returns "org.fdroid.fdroid"
        every { localSource.getInstaller("com.test.prop") } returns "com.android.vending"

        // Act
        val result = repo.scanAndClassify().first()

        // Assert
        // PROP has weight 1, FOSS has weight 4. PROP should come first.
        assertEquals(2, result.size)
        assertEquals(AppStatus.PROP, result[0].status)
        assertEquals(AppStatus.FOSS, result[1].status)
    }
}
