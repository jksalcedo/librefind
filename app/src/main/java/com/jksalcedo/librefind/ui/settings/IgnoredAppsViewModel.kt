package com.jksalcedo.librefind.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.local.IgnoredAppEntity
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IgnoredAppsViewModel(
    private val repository: IgnoredAppsRepository,
    context: Context
) : ViewModel() {

    val ignoredApps: StateFlow<List<IgnoredAppEntity>> = repository.getIgnoredApps()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        val apps = ignoredApps.value
        apps.forEach { ignoredApp ->
            if (context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
                    .none { app ->
                        app.packageName == ignoredApp.packageName
                    }
            ) {
                removeApp(ignoredApp.packageName)
            }
        }
    }

    fun restoreApp(packageName: String) {
        viewModelScope.launch {
            repository.restoreApp(packageName)
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            repository.deleteApp(packageName)
        }
    }
}
