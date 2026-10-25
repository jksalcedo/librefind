package com.jksalcedo.librefind.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.gson.Gson
import com.jksalcedo.librefind.data.local.catalog.CatalogDao
import com.jksalcedo.librefind.data.local.catalog.CatalogSolutionEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogSyncMetadataEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogTargetEntity
import com.jksalcedo.librefind.data.remote.CatalogApiService
import com.jksalcedo.librefind.data.remote.model.CatalogIndexDto
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogRepositoryImplTest {

    private val context: Context = mockk()
    private val assetManager: AssetManager = mockk()
    private val catalogDao: CatalogDao = mockk(relaxed = true)
    private val catalogApiService: CatalogApiService = mockk()
    private val gson = Gson()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: CatalogRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { context.assets } returns assetManager
        coEvery { catalogDao.getTargetsCount() } returns 5

        repository = CatalogRepositoryImpl(
            context = context,
            catalogDao = catalogDao,
            catalogApiService = catalogApiService,
            gson = gson,
            ioDispatcher = testDispatcher,
            cdnBaseUrl = "https://example.com/cdn"
        )
    }

    @Test
    fun `ensureCatalogSeeded skips if catalog already has targets`() = runTest {
        coEvery { catalogDao.getTargetsCount() } returns 10

        repository.ensureCatalogSeeded()

        verify(exactly = 0) { context.assets }
    }

    @Test
    fun `ensureCatalogSeeded parses bundled camelCase bundle and uses index hash`() = runTest {
        coEvery { catalogDao.getTargetsCount() } returns 0

        val indexJson = """
            {
              "filename": "catalog-abc123.json.gz",
              "hash": "abc123hash",
              "timestamp": 1700000000
            }
        """.trimIndent()
        val bundleJson = """
            {
              "targets": [
                {"id": "t1", "packageName": "com.example.target", "name": "Target", "categories": ["utility"]}
              ],
              "solutions": [
                {"id": "s1", "packageName": "com.example.solution", "name": "Solution", "license": "GPL", "upvotes": 5, "downvotes": 1}
              ],
              "mappings": [
                {"targetId": "t1", "solutionId": "s1"}
              ]
            }
        """.trimIndent()

        every { assetManager.open("catalog-index.json") } returns
                ByteArrayInputStream(indexJson.toByteArray(Charsets.UTF_8))
        every { assetManager.open("catalog.json.gz") } returns
                ByteArrayInputStream(createGzipBytes(bundleJson))

        val targetsSlot = slot<List<CatalogTargetEntity>>()
        val solutionsSlot = slot<List<CatalogSolutionEntity>>()
        coEvery {
            catalogDao.applyCatalogSync(
                capture(targetsSlot),
                capture(solutionsSlot),
                any(),
                any(),
                any()
            )
        } just Runs

        repository.ensureCatalogSeeded()

        coVerify {
            catalogDao.applyCatalogSync(
                targets = any(),
                solutions = any(),
                mappings = any(),
                newHash = "abc123hash",
                timestamp = 1700000000L
            )
        }

        assertEquals("com.example.target", targetsSlot.captured.single().packageName)
        assertEquals("[\"utility\"]", targetsSlot.captured.single().categories)
        assertEquals("com.example.solution", solutionsSlot.captured.single().packageName)
        assertEquals("GPL", solutionsSlot.captured.single().license)
        assertEquals(5, solutionsSlot.captured.single().upvotes)
    }

    @Test
    fun `ensureCatalogSeeded does not clear catalog for an empty bundle`() = runTest {
        coEvery { catalogDao.getTargetsCount() } returns 0
        stubBundledCatalog(
            indexJson = """{"filename":"catalog-empty.json.gz","hash":"emptyhash","timestamp":1}""",
            bundleJson = """{"targets":[],"solutions":[],"mappings":[]}"""
        )

        repository.ensureCatalogSeeded()

        coVerify(exactly = 0) {
            catalogDao.applyCatalogSync(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `ensureCatalogSeeded does not clear catalog for a malformed bundle`() = runTest {
        coEvery { catalogDao.getTargetsCount() } returns 0
        stubBundledCatalog(
            indexJson = """{"filename":"catalog-broken.json.gz","hash":"brokenhash","timestamp":1}""",
            bundleJson = """{"targets": ["""
        )

        repository.ensureCatalogSeeded()

        coVerify(exactly = 0) {
            catalogDao.applyCatalogSync(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `syncCatalogFromCdn returns false if hash matches current metadata`() = runTest {
        coEvery { catalogDao.getTargetsCount() } returns 5
        coEvery { catalogDao.getCatalogMetadata() } returns CatalogSyncMetadataEntity(contentHash = "hash123", timestamp = 1000L)
        coEvery { catalogApiService.getCatalogIndex("https://example.com/cdn/catalog-index.json") } returns CatalogIndexDto(filename = "catalog-hash123.json.gz", hash = "hash123", timestamp = 1000L)

        val result = repository.syncCatalogFromCdn()

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
        coVerify(exactly = 0) { catalogApiService.getCatalogBundle(any()) }
    }

    @Test
    fun `syncCatalogFromCdn downloads and applies catalog sync when hash is updated`() = runTest {
        coEvery { catalogDao.getTargetsCount() } returns 5
        coEvery { catalogDao.getCatalogMetadata() } returns CatalogSyncMetadataEntity(contentHash = "oldHash", timestamp = 500L)
        coEvery { catalogApiService.getCatalogIndex("https://example.com/cdn/catalog-index.json") } returns CatalogIndexDto(filename = "catalog-newHash.json.gz", hash = "newHash", timestamp = 1000L)

        val jsonPayload = """
            {
              "targets": [{"id": "t1", "packageName": "com.example.target", "name": "Target", "categories": ["utility"]}],
              "solutions": [{"id": "s1", "packageName": "com.example.solution", "name": "Solution", "upvotes": 5, "downvotes": 0}],
              "mappings": [{"targetId": "t1", "solutionId": "s1"}]
            }
        """.trimIndent()

        val gzipBytes = createGzipBytes(jsonPayload)
        // Raw gzip body: the repository must decompress it exactly once. A
        // Content-Encoding: gzip response would already be decompressed by OkHttp,
        // so this test also guards against accidental double-decompression.
        val responseBody = gzipBytes.toResponseBody()

        coEvery { catalogApiService.getCatalogBundle("https://example.com/cdn/catalog-newHash.json.gz") } returns responseBody

        val result = repository.syncCatalogFromCdn()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
        coVerify {
            catalogDao.applyCatalogSync(
                targets = any(),
                solutions = any(),
                mappings = any(),
                newHash = "newHash",
                timestamp = 1000L
            )
        }
    }

    private fun stubBundledCatalog(indexJson: String, bundleJson: String) {
        every { assetManager.open("catalog-index.json") } returns
                ByteArrayInputStream(indexJson.toByteArray(Charsets.UTF_8))
        every { assetManager.open("catalog.json.gz") } returns
                ByteArrayInputStream(createGzipBytes(bundleJson))
    }

    private fun createGzipBytes(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzos ->
            gzos.write(content.toByteArray(Charsets.UTF_8))
        }
        return bos.toByteArray()
    }
}
