package com.jksalcedo.librefind

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jksalcedo.librefind.data.local.PreferencesManager
import com.jksalcedo.librefind.di.appModule
import com.jksalcedo.librefind.di.networkModule
import com.jksalcedo.librefind.di.repositoryModule
import com.jksalcedo.librefind.di.supabaseModule
import com.jksalcedo.librefind.di.useCaseModule
import com.jksalcedo.librefind.di.viewModelModule
import com.jksalcedo.librefind.domain.repository.CatalogRepository
import com.jksalcedo.librefind.worker.CatalogSyncWorker
import com.jksalcedo.librefind.worker.NotificationWorker
import com.jksalcedo.librefind.worker.SignerFeedWorker
import com.jksalcedo.librefind.worker.UpdateCheckWorker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform.getKoin
import java.util.concurrent.TimeUnit

class LibreFindApp : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val preferencesManager = PreferencesManager(this)
        Thread.setDefaultUncaughtExceptionHandler(
            com.jksalcedo.librefind.utils.LibreFindCrashHandler(
                context = this,
                preferencesManager = preferencesManager,
                defaultHandler = defaultHandler
            )
        )

        startKoin {
            androidLogger()
            androidContext(this@LibreFindApp)
            modules(
                appModule,
                networkModule,
                repositoryModule,
                useCaseModule,
                viewModelModule,
                supabaseModule
            )
        }

        GlobalScope.launch(Dispatchers.IO) {
            val catalogRepository = getKoin().get<CatalogRepository>()
            catalogRepository.ensureCatalogSeeded()
        }

        // Schedule background workers off the main thread — WorkManager.getInstance()
        // initializes its internal Room database on first call which blocks the main thread.
        GlobalScope.launch(Dispatchers.IO) {
            val prefs = PreferencesManager(this@LibreFindApp)
            if (prefs.getNetworkConsentGranted()) {
                scheduleSignerFeedUpdate()
                scheduleNotificationWorker()
                scheduleCatalogSync()

                if (prefs.getAutoUpdateEnabled()) {
                    scheduleAutoUpdateWorker()
                } else {
                    WorkManager.getInstance(this@LibreFindApp)
                        .cancelUniqueWork("app_update_check")
                }
            } else {
                WorkManager.getInstance(this@LibreFindApp)
                    .cancelUniqueWork("signer_feed_update")
                WorkManager.getInstance(this@LibreFindApp)
                    .cancelUniqueWork("notification_check")
                WorkManager.getInstance(this@LibreFindApp)
                    .cancelUniqueWork("app_update_check")
                WorkManager.getInstance(this@LibreFindApp)
                    .cancelUniqueWork("catalog_sync")
            }
        }
    }

    private fun scheduleAutoUpdateWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                1,
                TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "app_update_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun scheduleCatalogSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<CatalogSyncWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "catalog_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun scheduleSignerFeedUpdate() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<SignerFeedWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "signer_feed_update",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun scheduleNotificationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notification_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
