package com.jksalcedo.librefind.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.AppItem
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.model.SovereigntyScore
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

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val scanInventoryUseCase: ScanInventoryUseCase,
    private val ignoredAppsRepository: IgnoredAppsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<AppStatus?>(null)

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
                _statusFilter
            ) { apps, query, statusFilter ->
                val filtered = apps
                    .filter { app ->
                        query.isBlank() ||
                                app.label.contains(query, ignoreCase = true) ||
                                app.packageName.contains(query, ignoreCase = true)
                    }
                    .filter { app ->
                        statusFilter == null || app.status == statusFilter
                    }

                // Calculate score using ALL apps (including ignored ones)
                val score = calculateScore(apps)

                Triple(filtered, score, Triple(query, statusFilter, null))
            }.collect { (filteredApps, score, params) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        apps = filteredApps,
                        sovereigntyScore = score,
                        searchQuery = params.first,
                        statusFilter = params.second,
                        error = params.third
                    )
                }
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
}

data class DashboardState(
    val isLoading: Boolean = false,
    val apps: List<AppItem> = emptyList(),
    val sovereigntyScore: SovereigntyScore? = null,
    val searchQuery: String = "",
    val statusFilter: AppStatus? = null,
    val error: String? = null
)


