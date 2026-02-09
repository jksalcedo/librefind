package com.jksalcedo.librefind.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.local.PreferencesManager
import com.jksalcedo.librefind.domain.model.AppItem
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.model.SovereigntyScore
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import com.jksalcedo.librefind.domain.usecase.ScanInventoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppFilter {
    ALL,
    PROP_WITH_ALTERNATIVES,
    PROP_NO_ALTERNATIVES,
    FOSS_ONLY,
    UNKNOWN_ONLY,
    PENDING_ONLY,
    IGNORED_ONLY
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val scanInventoryUseCase: ScanInventoryUseCase,
    private val ignoredAppsRepository: IgnoredAppsRepository,
    private val appRepository: AppRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<AppStatus?>(null)
    private val _appFilter = MutableStateFlow(AppFilter.ALL)

    init {
        viewModelScope.launch {
            combine(
                combine(
                    refreshTrigger.onStart { emit(Unit) },
                    ignoredAppsRepository.getIgnoredPackageNames()
                ) { _, _ -> }.flatMapLatest {
                    _state.update { it.copy(isLoading = true, error = null) }
                    scanInventoryUseCase()
                },
                _searchQuery,
                _statusFilter,
                _appFilter
            ) { apps, query, statusFilter, appFilter ->
                val filtered = apps
                    .filter { app ->
                        query.isBlank() ||
                                app.label.contains(query, ignoreCase = true) ||
                                app.packageName.contains(query, ignoreCase = true)
                    }
                    .filter { app ->
                        statusFilter == null || app.status == statusFilter
                    }
                    .filter { app ->
                        if (statusFilter == AppStatus.IGNORED) {
                            app.status == AppStatus.IGNORED
                        } else {
                            when (appFilter) {
                                AppFilter.ALL -> app.status != AppStatus.IGNORED
                                AppFilter.PROP_WITH_ALTERNATIVES -> app.status == AppStatus.PROP && app.knownAlternatives > 0
                                AppFilter.PROP_NO_ALTERNATIVES -> app.status == AppStatus.PROP && app.knownAlternatives == 0
                                AppFilter.FOSS_ONLY -> app.status == AppStatus.FOSS
                                AppFilter.UNKNOWN_ONLY -> app.status == AppStatus.UNKN
                                AppFilter.PENDING_ONLY -> app.status == AppStatus.PENDING
                                AppFilter.IGNORED_ONLY -> app.status == AppStatus.IGNORED
                            }
                        }
                    }

                val score = calculateScore(apps)
                Triple(filtered, score, Triple(query, statusFilter, appFilter))
            }.collect { (filteredApps, score, params) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        apps = filteredApps,
                        sovereigntyScore = score,
                        searchQuery = params.first,
                        statusFilter = params.second,
                        appFilter = params.third,
                        error = null
                    )
                }

                score.let { submitStats(it) }
            }
        }
    }

    fun scan() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: AppStatus?) {
        _statusFilter.value = status
        _appFilter.value = AppFilter.ALL
    }

    fun setAppFilter(filter: AppFilter) {
        _appFilter.value = filter
        _statusFilter.value = null
    }

    fun ignoreApp(packageName: String) {
        viewModelScope.launch {
            ignoredAppsRepository.ignoreApp(packageName)
        }
    }

    fun restoreApp(packageName: String) {
        viewModelScope.launch {
            ignoredAppsRepository.restoreApp(packageName)
        }
    }

    private fun calculateScore(apps: List<AppItem>): SovereigntyScore {
        val totalApps = apps.size
        val fossCount = apps.count { it.status == AppStatus.FOSS }
        val propCount = apps.count { it.status == AppStatus.PROP }
        val unknownCount = apps.count { it.status == AppStatus.UNKN }
        val ignoredCount = apps.count { it.status == AppStatus.IGNORED }

        return SovereigntyScore(
            totalApps = totalApps,
            fossCount = fossCount,
            proprietaryCount = propCount,
            unknownCount = unknownCount,
            ignoredCount = ignoredCount
        )
    }

    private fun submitStats(score: SovereigntyScore) {
        viewModelScope.launch {
            val deviceId = preferencesManager.getOrCreateDeviceId()
            appRepository.submitScanStats(
                deviceId = deviceId,
                fossCount = score.fossCount,
                proprietaryCount = score.proprietaryCount,
                unknownCount = score.unknownCount,
                appVersion = preferencesManager.getAppVersion()
            )
        }
    }
}

data class DashboardState(
    val isLoading: Boolean = false,
    val apps: List<AppItem> = emptyList(),
    val sovereigntyScore: SovereigntyScore? = null,
    val searchQuery: String = "",
    val statusFilter: AppStatus? = null,
    val appFilter: AppFilter = AppFilter.ALL,
    val error: String? = null
)
