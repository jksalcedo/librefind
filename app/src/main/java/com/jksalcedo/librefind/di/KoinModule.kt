package com.jksalcedo.librefind.di

import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.jksalcedo.librefind.data.local.AppDatabase
import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.data.local.PackageNameHeuristicsDb
import com.jksalcedo.librefind.data.local.PreferencesManager
import com.jksalcedo.librefind.data.local.SignerFeedDataStore
import com.jksalcedo.librefind.data.local.TrustedRomSignerDb
import com.jksalcedo.librefind.data.remote.SignerApiService
import com.jksalcedo.librefind.data.remote.UpdateApiService
import com.jksalcedo.librefind.data.repository.CacheRepositoryImpl
import com.jksalcedo.librefind.data.repository.DeviceInventoryRepoImpl
import com.jksalcedo.librefind.data.repository.IgnoredAppsRepositoryImpl
import com.jksalcedo.librefind.data.repository.ReclassifiedAppsRepositoryImpl
import com.jksalcedo.librefind.data.repository.UpdateRepositoryImpl
import com.jksalcedo.librefind.domain.repository.CacheRepository
import com.jksalcedo.librefind.domain.repository.DeviceInventoryRepo
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import com.jksalcedo.librefind.domain.repository.ReclassifiedAppsRepository
import com.jksalcedo.librefind.domain.repository.UpdateRepository
import com.jksalcedo.librefind.domain.usecase.GetAlternativeUseCase
import com.jksalcedo.librefind.domain.usecase.ScanInventoryUseCase
import com.jksalcedo.librefind.domain.usecase.SubmitProposalUseCase
import com.jksalcedo.librefind.domain.usecase.UpdateSubmissionUseCase
import com.jksalcedo.librefind.ui.auth.AuthViewModel
import com.jksalcedo.librefind.ui.correction.SuggestCorrectionViewModel
import com.jksalcedo.librefind.ui.dashboard.DashboardViewModel
import com.jksalcedo.librefind.ui.details.AlternativeDetailViewModel
import com.jksalcedo.librefind.ui.details.DetailsViewModel
import com.jksalcedo.librefind.ui.discover.DiscoverViewModel
import com.jksalcedo.librefind.ui.mysubmissions.MySubmissionsViewModel
import com.jksalcedo.librefind.ui.reports.MyReportsViewModel
import com.jksalcedo.librefind.ui.reports.ReportViewModel
import com.jksalcedo.librefind.ui.settings.IgnoredAppsViewModel
import com.jksalcedo.librefind.ui.settings.SettingsViewModel
import com.jksalcedo.librefind.ui.submit.SubmitViewModel
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    single { Dispatchers.IO }
    single { Dispatchers.Main }
    single { Dispatchers.Default }

    single { PreferencesManager(androidContext()) }
    single { InventorySource(androidContext(), get()) }
    single { PackageNameHeuristicsDb() }
    single { SignerFeedDataStore(androidContext(), get()) }

    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().ignoredAppDao() }
    single { get<AppDatabase>().appCacheDao() }
    single { get<AppDatabase>().reclassifiedAppDao() }
    single<IgnoredAppsRepository> { IgnoredAppsRepositoryImpl(get()) }
    single<ReclassifiedAppsRepository> { ReclassifiedAppsRepositoryImpl(get()) }
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

    single {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }

    single { get<Retrofit>().create(SignerApiService::class.java) }

    single {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
        retrofit.create(UpdateApiService::class.java)
    }
}

val repositoryModule = module {
    single<CacheRepository> { CacheRepositoryImpl(get(), get()) }
    single { TrustedRomSignerDb(get(), get()) }
    single<UpdateRepository> { UpdateRepositoryImpl(androidContext(), get()) }
    single<DeviceInventoryRepo> {
        DeviceInventoryRepoImpl(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
}

val useCaseModule = module {
    single { GetAlternativeUseCase(get()) }
    single { ScanInventoryUseCase(get()) }
    single { SubmitProposalUseCase(get()) }
    single { UpdateSubmissionUseCase(get()) }
}

val viewModelModule = module {
    viewModel { DetailsViewModel(get(), get(), get(), get()) }
    viewModel { AlternativeDetailViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { SubmitViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { MySubmissionsViewModel(get(), get()) }
    viewModel { IgnoredAppsViewModel(get(), get()) }
    viewModel {
        SettingsViewModel(
            get(), get(), get(), get()
        )
    }
    viewModel { ReportViewModel(get(), get()) }
    viewModel { MyReportsViewModel(get(), get()) }
    viewModel { DiscoverViewModel(get()) }
    viewModel { SuggestCorrectionViewModel(get()) }
}
