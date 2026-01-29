package com.jksalcedo.librefind.data.repository

import com.jksalcedo.librefind.data.local.IgnoredAppDao
import com.jksalcedo.librefind.data.local.IgnoredAppEntity
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import kotlinx.coroutines.flow.Flow

class IgnoredAppsRepositoryImpl(
    private val dao: IgnoredAppDao
) : IgnoredAppsRepository {

    override fun getIgnoredApps(): Flow<List<IgnoredAppEntity>> = dao.getAllIgnored()

    override fun getIgnoredPackageNames(): Flow<List<String>> = dao.getAllIgnoredPackageNames()

    override suspend fun ignoreApp(packageName: String) {
        dao.insert(IgnoredAppEntity(packageName))
    }

    override suspend fun restoreApp(packageName: String) {
        dao.delete(packageName)
    }

    override suspend fun deleteApp(packageName: String) {
        dao.delete(packageName)
    }
}
