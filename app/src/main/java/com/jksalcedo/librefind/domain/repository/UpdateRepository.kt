package com.jksalcedo.librefind.domain.repository

import com.jksalcedo.librefind.domain.model.AppUpdate

interface UpdateRepository {
    suspend fun checkForUpdate(): Result<AppUpdate>
    fun downloadUpdate(url: String, fileName: String): Long
}
