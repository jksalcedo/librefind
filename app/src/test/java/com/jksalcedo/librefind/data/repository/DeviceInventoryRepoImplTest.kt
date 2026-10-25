package com.jksalcedo.librefind.data.repository

import android.content.pm.PackageInfo
import android.util.Log
import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.data.local.PackageNameHeuristicsDb
import com.jksalcedo.librefind.data.local.TrustedRomSignerDb
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.CacheRepository
import com.jksalcedo.librefind.domain.repository.CatalogRepository
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import com.jksalcedo.librefind.domain.repository.ReclassifiedAppsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
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
    private val catalogRepository: CatalogRepository = mockk(relaxed = true)

    private lateinit var repo: DeviceInventoryRepoImpl

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any(), any<String>(), any<Throwable>()) } returns 0

        repo = DeviceInventoryRepoImpl(
            localSource, signatureDb, appRepository, ignoredAppsRepository,
            cacheRepository, reclassifiedAppsRepository, trustedRomSignerDb, catalogRepository
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
        coEvery { cacheRepository.hasAnyCache() } returns false
        coEvery { cacheRepository.getBulkCachedData() } returns Pair(emptyMap(), emptySet())
        coEvery { appRepository.areProprietary(any()) } returns emptyMap()
        coEvery { appRepository.areSolutions(any()) } returns emptySet()
        coEvery { appRepository.getPendingSubmissionPackages() } returns emptySet()
        coEvery { catalogRepository.ensureCatalogSeeded() } returns Unit
        coEvery { catalogRepository.getAllTargetPackageNames() } returns emptySet()
        coEvery { catalogRepository.getAllSolutionPackageNames() } returns emptySet()
        every { signatureDb.isAospSystemPackageName(any()) } returns false
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
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
        every { localSource.getLabelFromInfo(any()) } returns "FOSS App"
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
        every { localSource.getLabelFromInfo(any()) } returns "Prop App"
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
        every { localSource.getLabelFromInfo(any()) } returns "Reclassified App"
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
        every { localSource.getLabelFromInfo(pkgFoss.applicationInfo!!) } returns "FOSS"
        every { localSource.getLabelFromInfo(pkgProp.applicationInfo!!) } returns "PROP"
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

    @Test
    fun `scanAndClassify classifies catalog target as PROP when offline`() = runTest {
        // Arrange
        val pkg = mockk<PackageInfo> {
            packageName = "com.catalog.target"
            applicationInfo = mockk { flags = 0; icon = 0 }
        }
        coEvery { localSource.getRawApps() } returns listOf(pkg)
        every { localSource.getLabelFromInfo(any()) } returns "Catalog Target"
        every { localSource.getInstaller(any()) } returns null
        coEvery { cacheRepository.isCacheValid() } returns false
        coEvery { cacheRepository.hasAnyCache() } returns false
        coEvery { catalogRepository.getAllTargetPackageNames() } returns setOf("com.catalog.target")

        // Act
        val result = repo.scanAndClassify().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(AppStatus.PROP, result[0].status)
    }

    @Test
    fun `scanAndClassify classifies catalog solution as FOSS when offline`() = runTest {
        // Arrange
        val pkg = mockk<PackageInfo> {
            packageName = "com.catalog.solution"
            applicationInfo = mockk { flags = 0; icon = 0 }
        }
        coEvery { localSource.getRawApps() } returns listOf(pkg)
        every { localSource.getLabelFromInfo(any()) } returns "Catalog Solution"
        every { localSource.getInstaller(any()) } returns null
        coEvery { cacheRepository.isCacheValid() } returns false
        coEvery { cacheRepository.hasAnyCache() } returns false
        coEvery { catalogRepository.getAllSolutionPackageNames() } returns setOf("com.catalog.solution")

        // Act
        val result = repo.scanAndClassify().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(AppStatus.FOSS, result[0].status)
    }

    @Test
    fun `scanAndClassify prefers catalog solution over catalog target`() = runTest {
        // Arrange
        val pkg = mockk<PackageInfo> {
            packageName = "com.catalog.both"
            applicationInfo = mockk { flags = 0; icon = 0 }
        }
        coEvery { localSource.getRawApps() } returns listOf(pkg)
        every { localSource.getLabelFromInfo(any()) } returns "Both"
        every { localSource.getInstaller(any()) } returns null
        coEvery { cacheRepository.isCacheValid() } returns false
        coEvery { cacheRepository.hasAnyCache() } returns false
        coEvery { catalogRepository.getAllTargetPackageNames() } returns setOf("com.catalog.both")
        coEvery { catalogRepository.getAllSolutionPackageNames() } returns setOf("com.catalog.both")

        // Act
        val result = repo.scanAndClassify().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(AppStatus.FOSS, result[0].status)
    }

    @Test
    fun `scanAndClassify keeps installer behavior when package is absent from catalog`() = runTest {
        // Arrange
        val pkg = mockk<PackageInfo> {
            packageName = "com.test.offcatalog"
            applicationInfo = mockk { flags = 0; icon = 0 }
        }
        coEvery { localSource.getRawApps() } returns listOf(pkg)
        every { localSource.getLabelFromInfo(any()) } returns "Off Catalog"
        every { localSource.getInstaller(any()) } returns "com.android.vending"

        // Act
        val result = repo.scanAndClassify().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(AppStatus.PROP, result[0].status)
    }

    @Test
    fun `scanAndClassify does not throw when catalog lookups fail`() = runTest {
        // Arrange
        val pkg = mockk<PackageInfo> {
            packageName = "com.test.catalogfailure"
            applicationInfo = mockk { flags = 0; icon = 0 }
        }
        coEvery { localSource.getRawApps() } returns listOf(pkg)
        every { localSource.getLabelFromInfo(any()) } returns "Catalog Failure"
        every { localSource.getInstaller(any()) } returns "org.fdroid.fdroid"
        coEvery { catalogRepository.getAllTargetPackageNames() } throws RuntimeException("boom")
        coEvery { catalogRepository.getAllSolutionPackageNames() } throws RuntimeException("boom")

        // Act
        val result = repo.scanAndClassify().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(AppStatus.FOSS, result[0].status)
    }
}
