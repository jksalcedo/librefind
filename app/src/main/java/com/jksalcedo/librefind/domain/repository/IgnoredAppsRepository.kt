package com.jksalcedo.librefind.domain.repository

import com.jksalcedo.librefind.data.local.IgnoredAppEntity
import kotlinx.coroutines.flow.Flow

interface IgnoredAppsRepository {
    fun getIgnoredApps(): Flow<List<IgnoredAppEntity>>
    fun getIgnoredPackageNames(): Flow<List<String>>
    suspend fun ignoreApp(packageName: String)
    suspend fun restoreApp(packageName: String)
}
