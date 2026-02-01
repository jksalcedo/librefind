package com.jksalcedo.librefind.data.local.cache.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_solutions")
data class CachedSolution(
    @PrimaryKey
    val packageName: String,
    val name: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
