package com.jksalcedo.librefind.domain.repository

import kotlinx.coroutines.flow.Flow

interface ReclassifiedAppsRepository {
    fun getReclassifiedPackageNames(): Flow<List<String>>
    suspend fun reclassifyAsFoss(packageName: String)
    suspend fun undoReclassify(packageName: String)
}
