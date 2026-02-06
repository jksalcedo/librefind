package com.jksalcedo.librefind.data.repository

import android.util.Log
import com.jksalcedo.librefind.data.local.cache.AppCacheDao
import com.jksalcedo.librefind.data.local.cache.entities.CachedSolution
import com.jksalcedo.librefind.data.local.cache.entities.CachedTarget
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CacheRepositoryImpl(
    private val appCacheDao: AppCacheDao,
    private val appRepository: AppRepository
) : CacheRepository {

    companion object {
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        private const val TAG = "CacheRepo"
    }

    override suspend fun refreshCache() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Refreshing cache from remote...")

                // Single bulk fetch
                val targetsWithCounts = appRepository.getProprietaryTargetsWithAlternativesCount()
                Log.d(TAG, "Fetched ${targetsWithCounts.size} targets from remote (bulk)")

                val now = System.currentTimeMillis()
                val cachedTargets = targetsWithCounts.map { (packageName, count) ->
                    CachedTarget(
                        packageName = packageName,
                        name = packageName,
                        alternativesCount = count,
                        lastUpdated = now
                    )
                }

                appCacheDao.clearTargets()
                appCacheDao.upsertTargets(cachedTargets)
                Log.d(TAG, "Cached ${cachedTargets.size} targets")

                // Also bulk-cache solutions
                val solutionPackages = appRepository.getAllSolutionPackageNames()
                val cachedSolutions = solutionPackages.map { packageName ->
                    CachedSolution(
                        packageName = packageName,
                        lastUpdated = now,
                        name = packageName
                    )
                }

                appCacheDao.clearSolutions()
                appCacheDao.upsertSolutions(cachedSolutions)
                Log.d(TAG, "Cached ${cachedSolutions.size} solutions")

                Log.d(TAG, "Cache refresh complete")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh cache", e)
                throw e
            }
        }
    }

    override suspend fun isCacheValid(): Boolean = withContext(Dispatchers.IO) {
        val lastUpdated = appCacheDao.getTargetsLastUpdated() ?: return@withContext false
        val age = System.currentTimeMillis() - lastUpdated
        val isValid = age < CACHE_TTL_MS
        Log.d(TAG, "Cache age: ${age / 1000 / 60}min, valid: $isValid")
        isValid
    }

    override suspend fun isTargetCached(packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            appCacheDao.getTarget(packageName) != null
        }

    override suspend fun isSolutionCached(packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            appCacheDao.getSolution(packageName) != null
        }

    override suspend fun getAlternativesCount(packageName: String): Int? =
        withContext(Dispatchers.IO) {
            appCacheDao.getAlternativesCount(packageName)
        }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            appCacheDao.clearTargets()
            appCacheDao.clearSolutions()
            Log.d(TAG, "Cache cleared")
        }
    }
}