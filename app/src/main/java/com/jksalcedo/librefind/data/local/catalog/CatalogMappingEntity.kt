package com.jksalcedo.librefind.data.local.catalog

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "catalog_mappings",
    primaryKeys = ["targetId", "solutionId"],
    foreignKeys = [
        ForeignKey(
            entity = CatalogTargetEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CatalogSolutionEntity::class,
            parentColumns = ["id"],
            childColumns = ["solutionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["targetId"]),
        Index(value = ["solutionId"])
    ]
)
data class CatalogMappingEntity(
    val targetId: String,
    val solutionId: String
)
