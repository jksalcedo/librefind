package com.jksalcedo.librefind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.local.PreferencesManager
import com.jksalcedo.librefind.ui.dashboard.components.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsState(
    val cacheSizeMB: String = "0.0 MB",
    val isClearing: Boolean = false,
    val showClearConfirmation: Boolean = false
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        calculateCacheSize()
    }

    fun calculateCacheSize() {
        viewModelScope.launch {
            val sizeBytes = withContext(Dispatchers.IO) {
                AppIconCache.getCacheSize()
            }
            val sizeMB = sizeBytes / (1024.0 * 1024.0)
            _state.value = _state.value.copy(
                cacheSizeMB = String.format("%.2f MB", sizeMB)
            )
        }
    }

    fun showClearConfirmation() {
        _state.value = _state.value.copy(showClearConfirmation = true)
    }

    fun hideClearConfirmation() {
        _state.value = _state.value.copy(showClearConfirmation = false)
    }

    fun clearCache() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClearing = true, showClearConfirmation = false)
            withContext(Dispatchers.IO) {
                AppIconCache.clearCache()
            }
            calculateCacheSize()
            _state.value = _state.value.copy(isClearing = false)
        }
    }
}
