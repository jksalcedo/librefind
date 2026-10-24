package com.jksalcedo.librefind.data.local.catalog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CatalogDao {

    @Query("DELETE FROM catalog_targets")
    suspend fun clearTargets()

    @Query("DELETE FROM catalog_solutions")
    suspend fun clearSolutions()

    @Query("DELETE FROM catalog_mappings")
    suspend fun clearMappings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<CatalogTargetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSolutions(solutions: List<CatalogSolutionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<CatalogMappingEntity>)

    @Query("SELECT COUNT(*) FROM catalog_targets")
    suspend fun getTargetsCount(): Int

    @Query("SELECT * FROM catalog_targets WHERE packageName = :packageName LIMIT 1")
    suspend fun getTargetByPackage(packageName: String): CatalogTargetEntity?

    @Query(
        """
        SELECT s.* FROM catalog_solutions s
        INNER JOIN catalog_mappings m ON s.id = m.solutionId
        WHERE m.targetId = :targetId
    """
    )
    suspend fun getSolutionsForTarget(targetId: String): List<CatalogSolutionEntity>

    @Query("SELECT * FROM catalog_solutions WHERE packageName = :packageName LIMIT 1")
    suspend fun getSolutionByPackage(packageName: String): CatalogSolutionEntity?

    @Query("SELECT packageName FROM catalog_targets")
    suspend fun getAllTargetPackageNames(): List<String>

    @Query("SELECT packageName FROM catalog_solutions")
    suspend fun getAllSolutionPackageNames(): List<String>

    // Metadata
    @Query("SELECT * FROM catalog_sync_metadata LIMIT 1")
    suspend fun getCatalogMetadata(): CatalogSyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCatalogMetadata(metadata: CatalogSyncMetadataEntity)

    @Transaction
    suspend fun applyCatalogSync(
        targets: List<CatalogTargetEntity>,
        solutions: List<CatalogSolutionEntity>,
        mappings: List<CatalogMappingEntity>,
        newHash: String,
        timestamp: Long
    ) {
        clearTargets()
        clearSolutions()
        clearMappings()

        insertTargets(targets)
        insertSolutions(solutions)
        insertMappings(mappings)

        setCatalogMetadata(CatalogSyncMetadataEntity(contentHash = newHash, timestamp = timestamp))
    }
}
