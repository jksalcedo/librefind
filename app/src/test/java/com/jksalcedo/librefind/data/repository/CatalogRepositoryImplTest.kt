package com.jksalcedo.librefind.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.gson.Gson
import com.jksalcedo.librefind.data.local.catalog.CatalogDao
import com.jksalcedo.librefind.data.local.catalog.CatalogSyncMetadataEntity
import com.jksalcedo.librefind.data.remote.CatalogApiService
import com.jksalcedo.librefind.data.remote.model.CatalogIndexDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
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

    private fun createGzipBytes(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzos ->
            gzos.write(content.toByteArray(Charsets.UTF_8))
        }
        return bos.toByteArray()
    }
}
