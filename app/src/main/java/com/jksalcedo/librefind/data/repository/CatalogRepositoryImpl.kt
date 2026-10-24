package com.jksalcedo.librefind.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.jksalcedo.librefind.data.local.catalog.CatalogDao
import com.jksalcedo.librefind.data.local.catalog.CatalogMappingEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogSolutionEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogTargetEntity
import com.jksalcedo.librefind.data.remote.CatalogApiService
import com.jksalcedo.librefind.data.remote.model.CatalogBundleDto
import com.jksalcedo.librefind.data.remote.model.CatalogIndexDto
import com.jksalcedo.librefind.domain.repository.CatalogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

class CatalogRepositoryImpl(
    private val context: Context,
    private val catalogDao: CatalogDao,
    private val catalogApiService: CatalogApiService,
    private val gson: Gson,
    private val ioDispatcher: CoroutineDispatcher,
    private val cdnBaseUrl: String = DEFAULT_CDN_BASE_URL
) : CatalogRepository {

    companion object {
        private const val TAG = "CatalogRepository"
        const val DEFAULT_CDN_BASE_URL =
            "https://6191600c54ad78570e7078d14a0182f3.r2.cloudflarestorage.com/librefind-cdn"
    }

    override suspend fun ensureCatalogSeeded(): Unit = withContext(ioDispatcher) {
        if (catalogDao.getTargetsCount() > 0) return@withContext

        try {
            val index = context.assets.open("catalog-index.json").use { stream ->
                gson.fromJson(
                    InputStreamReader(stream, Charsets.UTF_8),
                    CatalogIndexDto::class.java
                )
            }

            context.assets.open("catalog.json.gz").use { stream ->
                parseAndApplyCatalogStream(
                    inputStream = stream,
                    hash = index.hash,
                    timestamp = index.timestamp
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed catalog from local asset", e)
        }
    }

    override suspend fun syncCatalogFromCdn(): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            ensureCatalogSeeded()

            val indexUrl = "$cdnBaseUrl/catalog-index.json"
            Log.d(TAG, "Fetching catalog index from $indexUrl...")
            val indexDto = catalogApiService.getCatalogIndex(indexUrl)

            val currentMeta = catalogDao.getCatalogMetadata()
            if (currentMeta != null && currentMeta.contentHash == indexDto.hash) {
                Log.d(TAG, "Catalog is already up to date (hash: ${indexDto.hash})")
                return@runCatching false
            }

            val bundleUrl = "$cdnBaseUrl/${indexDto.filename}"
            Log.d(TAG, "Downloading catalog bundle from $bundleUrl...")
            val responseBody = catalogApiService.getCatalogBundle(bundleUrl)

            responseBody.byteStream().use { inputStream ->
                parseAndApplyCatalogStream(inputStream, indexDto.hash, indexDto.timestamp)
            }

            Log.d(TAG, "Successfully synced catalog from CDN (hash: ${indexDto.hash})")
            true
        }
    }

    override suspend fun getTargetByPackageName(packageName: String): CatalogTargetEntity? =
        withContext(ioDispatcher) {
            catalogDao.getTargetByPackage(packageName)
        }

    override suspend fun getSolutionsForTarget(targetId: String): List<CatalogSolutionEntity> =
        withContext(ioDispatcher) {
            catalogDao.getSolutionsForTarget(targetId)
        }

    override suspend fun getSolutionByPackageName(packageName: String): CatalogSolutionEntity? =
        withContext(ioDispatcher) {
            catalogDao.getSolutionByPackage(packageName)
        }

    private suspend fun parseAndApplyCatalogStream(
        inputStream: InputStream,
        hash: String,
        timestamp: Long
    ) {
        val gzipStream = withContext(Dispatchers.IO) {
            GZIPInputStream(inputStream)
        }
        val reader = InputStreamReader(gzipStream, Charsets.UTF_8)
        val bundleDto = gson.fromJson(reader, CatalogBundleDto::class.java) ?: return

        val targets = bundleDto.targets.map { target ->
            CatalogTargetEntity(
                id = target.id,
                packageName = target.packageName,
                name = target.name,
                categories = gson.toJson(target.categories)
            )
        }

        val solutions = bundleDto.solutions.map { sol ->
            CatalogSolutionEntity(
                id = sol.id,
                packageName = sol.packageName,
                name = sol.name,
                description = sol.description,
                license = sol.license,
                iconUrl = sol.iconUrl,
                websiteUrl = sol.websiteUrl,
                repositoryUrl = sol.repositoryUrl,
                fdroidUrl = sol.fdroidUrl,
                upvotes = sol.upvotes,
                downvotes = sol.downvotes
            )
        }

        val mappings = bundleDto.mappings.map { map ->
            CatalogMappingEntity(
                targetId = map.targetId,
                solutionId = map.solutionId
            )
        }

        catalogDao.applyCatalogSync(targets, solutions, mappings, hash, timestamp)
    }
}
