package com.jksalcedo.librefind.data.repository

import com.jksalcedo.librefind.data.local.ReclassifiedAppDao
import com.jksalcedo.librefind.data.local.ReclassifiedAppEntity
import com.jksalcedo.librefind.domain.repository.ReclassifiedAppsRepository
import kotlinx.coroutines.flow.Flow

class ReclassifiedAppsRepositoryImpl(
    private val dao: ReclassifiedAppDao
) : ReclassifiedAppsRepository {

    override fun getReclassifiedPackageNames(): Flow<List<String>> =
        dao.getAllReclassifiedPackageNames()

    override suspend fun reclassifyAsFoss(packageName: String) {
        dao.insert(ReclassifiedAppEntity(packageName))
    }

    override suspend fun undoReclassify(packageName: String) {
        dao.delete(packageName)
    }
}
