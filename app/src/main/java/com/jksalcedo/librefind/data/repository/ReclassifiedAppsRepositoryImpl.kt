package com.jksalcedo.librefind.data.repository

import com.jksalcedo.librefind.data.local.ReclassifiedAppDao
import com.jksalcedo.librefind.data.local.ReclassifiedAppEntity
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.repository.ReclassifiedAppsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReclassifiedAppsRepositoryImpl(
    private val dao: ReclassifiedAppDao
) : ReclassifiedAppsRepository {

    override fun getReclassifiedApps(): Flow<Map<String, AppStatus>> {
        return dao.getAllReclassifiedApps().map { list ->
            list.associate { it.packageName to it.status }
        }
    }

    override suspend fun reclassifyApp(packageName: String, status: AppStatus) {
        dao.insert(ReclassifiedAppEntity(packageName, status))
    }

    override suspend fun undoReclassify(packageName: String) {
        dao.delete(packageName)
    }
}
