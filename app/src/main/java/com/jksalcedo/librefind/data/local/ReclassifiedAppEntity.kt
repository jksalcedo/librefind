package com.jksalcedo.librefind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reclassified_apps")
data class ReclassifiedAppEntity(
    @PrimaryKey
    val packageName: String
)
