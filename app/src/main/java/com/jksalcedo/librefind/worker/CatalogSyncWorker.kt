package com.jksalcedo.librefind.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jksalcedo.librefind.domain.repository.CatalogRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CatalogSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val catalogRepository: CatalogRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            catalogRepository.ensureCatalogSeeded()
            val syncResult = catalogRepository.syncCatalogFromCdn()
            if (syncResult.isSuccess) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}