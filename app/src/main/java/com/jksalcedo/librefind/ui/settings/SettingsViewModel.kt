package com.jksalcedo.librefind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.local.PreferencesManager
import com.jksalcedo.librefind.domain.repository.AuthRepository
import com.jksalcedo.librefind.ui.dashboard.components.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class SettingsState(
    val cacheSizeMB: String = "0.0 MB",
    val isClearing: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isAccountDeleted: Boolean = false,
    val deleteAccountError: String? = null
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository
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
            _state.update {
                it.copy(
                    cacheSizeMB = String.format(
                        Locale.getDefault(),
                        "%.2f MB",
                        sizeMB
                    )
                )
            }
        }
    }

    fun showClearConfirmation() {
        _state.update { it.copy(showClearConfirmation = true) }
    }

    fun hideClearConfirmation() {
        _state.update { it.copy(showClearConfirmation = false) }
    }

    fun clearCache() {
        viewModelScope.launch {
            _state.update { it.copy(isClearing = true, showClearConfirmation = false) }
            withContext(Dispatchers.IO) {
                AppIconCache.clearCache()
            }
            calculateCacheSize()
            _state.update { it.copy(isClearing = false) }
        }
    }

    // --- Account Deletion ---

    fun showDeleteAccountConfirmation() {
        _state.update { it.copy(showDeleteAccountConfirmation = true) }
    }

    fun hideDeleteAccountConfirmation() {
        _state.update { it.copy(showDeleteAccountConfirmation = false) }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDeletingAccount = true,
                    showDeleteAccountConfirmation = false
                )
            }
            try {
                authRepository.signOut()
                _state.update { it.copy(isDeletingAccount = false, isAccountDeleted = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isDeletingAccount = false,
                        deleteAccountError = e.message ?: "Failed to delete account"
                    )
                }
            }
        }
    }

    fun clearDeleteAccountError() {
        _state.update { it.copy(deleteAccountError = null) }
    }
}