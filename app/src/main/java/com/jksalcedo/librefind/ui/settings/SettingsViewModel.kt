package com.jksalcedo.librefind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.local.PreferencesManager
import com.jksalcedo.librefind.domain.model.AppUpdate
import com.jksalcedo.librefind.domain.repository.AuthRepository
import com.jksalcedo.librefind.domain.repository.UpdateRepository
import com.jksalcedo.librefind.ui.dashboard.components.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class UpdateCheckStatus {
    IDLE, CHECKING, UPDATE_AVAILABLE, UP_TO_DATE, ERROR
}

data class SettingsState(
    val cacheSizeMB: String = "0.0 MB",
    val classificationCacheCount: Int = 0,
    val isClearing: Boolean = false,
    val isClearingClassification: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val showClearClassificationConfirmation: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isAccountDeleted: Boolean = false,
    val deleteAccountError: String? = null,
    val updateCheckStatus: UpdateCheckStatus = UpdateCheckStatus.IDLE,
    val latestUpdate: AppUpdate? = null,
    val updateError: String? = null,
    val isLoggedIn: Boolean = false
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository,
    private val updateRepository: UpdateRepository,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        calculateCacheSize()
        calculateClassificationCacheCount()
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _state.update { it.copy(isLoggedIn = user != null) }
            }
        }
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

    fun calculateClassificationCacheCount() {
        viewModelScope.launch {
            val count = cacheRepository.getTotalCachedItems()
            _state.update { it.copy(classificationCacheCount = count) }
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

    fun showClearClassificationConfirmation() {
        _state.update { it.copy(showClearClassificationConfirmation = true) }
    }

    fun hideClearClassificationConfirmation() {
        _state.update { it.copy(showClearClassificationConfirmation = false) }
    }

    fun clearClassificationCache() {
        viewModelScope.launch {
            _state.update { it.copy(isClearingClassification = true, showClearClassificationConfirmation = false) }
            cacheRepository.clearCache()
            calculateClassificationCacheCount()
            _state.update { it.copy(isClearingClassification = false) }
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

    // --- Auto-Updater ---

    fun checkForUpdates() {
        viewModelScope.launch {
            _state.update { it.copy(updateCheckStatus = UpdateCheckStatus.CHECKING) }
            updateRepository.checkForUpdate()
                .onSuccess { update ->
                    _state.update {
                        it.copy(
                            updateCheckStatus = if (update.isUpdateAvailable) UpdateCheckStatus.UPDATE_AVAILABLE else UpdateCheckStatus.UP_TO_DATE,
                            latestUpdate = update
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            updateCheckStatus = UpdateCheckStatus.ERROR,
                            updateError = error.message ?: "Failed to check for updates"
                        )
                    }
                }
        }
    }

    fun downloadUpdate() {
        val update = _state.value.latestUpdate ?: return
        updateRepository.downloadUpdate(update.downloadUrl, update.fileName)
        resetUpdateStatus()
    }

    fun resetUpdateStatus() {
        _state.update { it.copy(updateCheckStatus = UpdateCheckStatus.IDLE, latestUpdate = null, updateError = null) }
    }
}