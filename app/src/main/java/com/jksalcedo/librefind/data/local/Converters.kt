package com.jksalcedo.librefind.data.local

import androidx.room.TypeConverter
import com.jksalcedo.librefind.domain.model.AppStatus

class Converters {
    @TypeConverter
    fun toAppStatus(value: String): AppStatus {
        return try {
            AppStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AppStatus.FOSS
        }
    }

    @TypeConverter
    fun fromAppStatus(status: AppStatus): String {
        return status.name
    }
}
