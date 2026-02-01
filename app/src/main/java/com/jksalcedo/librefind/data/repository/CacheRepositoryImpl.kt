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
                
                val targetPackages = appRepository.getProprietaryTargets()
                Log.d(TAG, "Fetched ${targetPackages.size} targets from remote")
                
                val cachedTargets = targetPackages.map { packageName ->
                    val count = try {
                        appRepository.getAlternativesCount(packageName)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get alternatives count for $packageName", e)
                        0
                    }
                    
                    CachedTarget(
                        packageName = packageName,
                        name = packageName,
                        alternativesCount = count,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                
                appCacheDao.clearTargets()
                appCacheDao.upsertTargets(cachedTargets)
                Log.d(TAG, "Cached ${cachedTargets.size} targets")
                
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
