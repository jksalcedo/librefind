package com.jksalcedo.librefind.domain.repository

interface CacheRepository {
    suspend fun refreshCache()
    suspend fun isCacheValid(): Boolean
    suspend fun isTargetCached(packageName: String): Boolean
    suspend fun isSolutionCached(packageName: String): Boolean
    suspend fun getAlternativesCount(packageName: String): Int?
    suspend fun clearCache()
}
