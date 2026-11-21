package com.jksalcedo.librefind.data.local.catalog

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalog_targets",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class CatalogTargetEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val name: String,
    // use a TypeConverter for categories
    val categories: String
)
