package com.jksalcedo.librefind.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.UserProfile
import com.jksalcedo.librefind.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val topContributors: List<UserProfile> = emptyList(),
    val error: String? = null
)

class LeaderboardViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        fetchLeaderboard()
    }

    fun fetchLeaderboard() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.topContributors
            .onEach { contributors ->
                _uiState.update { it.copy(isLoading = false, topContributors = contributors) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }
}
