package com.jksalcedo.librefind.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jksalcedo.librefind.data.local.TrustedRomSignerDb
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SignerFeedWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val signerDb: TrustedRomSignerDb by inject()

    override suspend fun doWork(): Result {
        return try {
            signerDb.refreshFeed()
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
