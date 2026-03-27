package com.jksalcedo.librefind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jksalcedo.librefind.domain.model.AppStatus

@Entity(tableName = "reclassified_apps")
data class ReclassifiedAppEntity(
    @PrimaryKey
    val packageName: String,
    val status: AppStatus = AppStatus.FOSS
)
