package com.jksalcedo.librefind.data.local.catalog

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_sync_metadata")
data class CatalogSyncMetadataEntity(
    @PrimaryKey
    val id: Int = 1, // Single row entity
    val contentHash: String,
    val timestamp: Long
)
