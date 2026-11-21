package com.jksalcedo.librefind.data.local.catalog

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalog_solutions",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class CatalogSolutionEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val name: String,
    val description: String?,
    val license: String?,
    val iconUrl: String?,
    val websiteUrl: String?,
    val repositoryUrl: String?,
    val fdroidUrl: String?,
    val upvotes: Int,
    val downvotes: Int
)
