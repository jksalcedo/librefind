package com.jksalcedo.librefind.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.local.IgnoredAppEntity
import com.jksalcedo.librefind.domain.repository.IgnoredAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IgnoredAppsViewModel(
    private val repository: IgnoredAppsRepository,
    context: Context
) : ViewModel() {

    val ignoredApps: StateFlow<List<IgnoredAppEntity>> = repository.getIgnoredApps()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Clean up ignored apps that are no longer installed on the device
        viewModelScope.launch {
            val installedPackageNames = withContext(Dispatchers.IO) {
                try {
                    context.packageManager
                        .getInstalledPackages(PackageManager.GET_META_DATA)
                        .map { it.packageName }
                        .toSet()
                } catch (_: Exception) {
                    emptySet()
                }
            }

            if (installedPackageNames.isEmpty()) return@launch

            //wait
            val currentIgnored = withContext(Dispatchers.IO) {
                repository.getIgnoredApps().first()
            }

            currentIgnored.forEach { ignoredApp ->
                if (ignoredApp.packageName !in installedPackageNames) {
                    removeApp(ignoredApp.packageName)
                }
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