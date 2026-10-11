package com.jksalcedo.librefind.data.local.user

import androidx.room.Entity

@Entity(tableName = "user_custom_mappings", primaryKeys = ["targetPkg", "solutionPkg"])
data class UserCustomMappingEntity(
    val targetPkg: String,
    val solutionPkg: String,
    val createdAt: Long = System.currentTimeMillis()
)
