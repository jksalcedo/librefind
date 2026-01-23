package com.jksalcedo.librefind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.local.IgnoredAppEntity
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IgnoredAppsViewModel(
    private val repository: IgnoredAppsRepository
) : ViewModel() {

    val ignoredApps: StateFlow<List<IgnoredAppEntity>> = repository.getIgnoredApps()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun restoreApp(packageName: String) {
        viewModelScope.launch {
            repository.restoreApp(packageName)
        }
    }
}
