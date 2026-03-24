package com.jksalcedo.librefind.domain.repository

import com.jksalcedo.librefind.domain.model.AppStatus
import kotlinx.coroutines.flow.Flow

interface ReclassifiedAppsRepository {
    fun getReclassifiedApps(): Flow<Map<String, AppStatus>>
    suspend fun reclassifyApp(packageName: String, status: AppStatus)
    suspend fun undoReclassify(packageName: String)
}
