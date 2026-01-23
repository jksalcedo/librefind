package com.jksalcedo.librefind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ignored_apps")
data class IgnoredAppEntity(
    @PrimaryKey
    val packageName: String
)
