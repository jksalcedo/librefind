package com.jksalcedo.librefind.domain.repository

import com.jksalcedo.librefind.data.local.catalog.CatalogSolutionEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogTargetEntity

interface CatalogRepository {
    suspend fun ensureCatalogSeeded()
    suspend fun syncCatalogFromCdn(): Result<Boolean>
    suspend fun getTargetByPackageName(packageName: String): CatalogTargetEntity?
    suspend fun getSolutionsForTarget(targetId: String): List<CatalogSolutionEntity>
    suspend fun getSolutionByPackageName(packageName: String): CatalogSolutionEntity?
    suspend fun getAllTargetPackageNames(): Set<String>
    suspend fun getAllSolutionPackageNames(): Set<String>
}
