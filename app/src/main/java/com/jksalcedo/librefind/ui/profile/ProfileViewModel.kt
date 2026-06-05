package com.jksalcedo.librefind.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.model.SubmissionStatus
import com.jksalcedo.librefind.domain.model.UserProfile
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val submissions: List<Submission> = emptyList(),
    val isOwnProfile: Boolean = false,
    val error: String? = null,
    val isDeletingAccount: Boolean = false,
    val accountDeleted: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentUser = authRepository.getCurrentUser()
            
            val targetUserId = userId ?: currentUser?.uid
            
            if (targetUserId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not found") }
                return@launch
            }

            val isOwn = targetUserId == currentUser?.uid
            
            try {
                val profile = if (isOwn) currentUser else authRepository.getPublicProfile(targetUserId)
                
                if (profile == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Profile not found") }
                    return@launch
                }

                // For own profile, show all. For public, show only approved.
                val submissions = appRepository.getUserSubmissions(
                    targetUserId, 
                    if (isOwn) null else SubmissionStatus.APPROVED
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        submissions = submissions,
                        isOwnProfile = isOwn
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load profile") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }
            authRepository.deleteAccount()
                .onSuccess {
                    _uiState.update { it.copy(isDeletingAccount = false, accountDeleted = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isDeletingAccount = false, error = e.message) }
                }
        }
    }
}
