package com.jksalcedo.librefind.di

import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.jksalcedo.librefind.data.local.AppDatabase
import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.data.local.SafeSignatureDb
import com.jksalcedo.librefind.data.repository.DeviceInventoryRepoImpl
import com.jksalcedo.librefind.data.repository.IgnoredAppsRepositoryImpl
import com.jksalcedo.librefind.domain.repository.DeviceInventoryRepo
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import com.jksalcedo.librefind.domain.usecase.GetAlternativeUseCase
import com.jksalcedo.librefind.domain.usecase.ScanInventoryUseCase
import com.jksalcedo.librefind.ui.auth.AuthViewModel
import com.jksalcedo.librefind.ui.dashboard.DashboardViewModel
import com.jksalcedo.librefind.ui.details.AlternativeDetailViewModel
import com.jksalcedo.librefind.ui.details.DetailsViewModel
import com.jksalcedo.librefind.ui.mysubmissions.MySubmissionsViewModel
import com.jksalcedo.librefind.ui.settings.IgnoredAppsViewModel
import com.jksalcedo.librefind.ui.submit.SubmitViewModel
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    single { Dispatchers.IO }
    single { Dispatchers.Main }
    single { Dispatchers.Default }

    single { InventorySource(androidContext()) }
    single { SafeSignatureDb() }

    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().ignoredAppDao() }
    single<IgnoredAppsRepository> { IgnoredAppsRepositoryImpl(get()) }
}

val networkModule = module {
    single {
        GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create()
    }

    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .writeTimeout(30L, TimeUnit.SECONDS)
            .build()
    }
}

val repositoryModule = module {
    single<DeviceInventoryRepo> { DeviceInventoryRepoImpl(get(), get(), get(), get()) }
}

val useCaseModule = module {
    single { GetAlternativeUseCase(get()) }
    single { ScanInventoryUseCase(get()) }
    single { com.jksalcedo.librefind.domain.usecase.SubmitProposalUseCase(get()) }
    single { com.jksalcedo.librefind.domain.usecase.UpdateSubmissionUseCase(get()) }
}

val viewModelModule = module {
    viewModel { DetailsViewModel(get(), get(), get(), get()) }
    viewModel { AlternativeDetailViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { SubmitViewModel(get(), get(), get(), get(), get()) }
    viewModel { MySubmissionsViewModel(get(), get()) }
    viewModel { IgnoredAppsViewModel(get(), get()) }
}


