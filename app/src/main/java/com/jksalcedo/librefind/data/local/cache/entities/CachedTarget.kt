package com.jksalcedo.librefind.data.local.cache.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_targets")
data class CachedTarget(
    @PrimaryKey
    val packageName: String,
    val name: String,
    val alternativesCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)
