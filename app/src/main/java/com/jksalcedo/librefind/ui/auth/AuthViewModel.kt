package com.jksalcedo.librefind.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.UserProfile
import com.jksalcedo.librefind.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignedIn: Boolean = false,
    val needsProfileSetup: Boolean = false,
    val profileComplete: Boolean = false,
    val userProfile: UserProfile? = null,
    val showCheckEmailDialog: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        isSignedIn = true,
                        needsProfileSetup = user.username.isBlank(),
                        profileComplete = user.username.isNotBlank(),
                        userProfile = user
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSignedIn = false,
                        needsProfileSetup = false,
                        profileComplete = false,
                        userProfile = null
                    )
                }
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signUp(email, password, username)
                .onSuccess {
                    val currentUser = authRepository.getCurrentUser()

                    if (currentUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showCheckEmailDialog = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSignedIn = true,
                            needsProfileSetup = true
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = sanitizeAuthError(e.message)
                    )
                }

        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signIn(email, password)
                .onSuccess {
                    // Current user flow will update state
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = sanitizeAuthError(e.message)
                    )
                }
        }
    }

    fun saveProfile(username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.updateProfile(username)
                .onSuccess {
                    // Fetch updated profile or rely on flow
                    val updatedUser = authRepository.getCurrentUser()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsProfileSetup = false,
                        profileComplete = true,
                        userProfile = updatedUser
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = sanitizeAuthError(e.message)
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissCheckEmailDialog() {
        _uiState.value = _uiState.value.copy(showCheckEmailDialog = false)
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            // State update is handled by the flow collector in init block
        }
    }

    private fun sanitizeAuthError(rawMessage: String?): String {
        return when {
            rawMessage == null -> "An error occurred"
            rawMessage.contains("Invalid login credentials", ignoreCase = true) ->
                "Invalid email or password"
            rawMessage.contains("Email not confirmed", ignoreCase = true) ->
                "Please verify your email before signing in"
            rawMessage.contains("User already registered", ignoreCase = true) ->
                "An account with this email already exists"
            rawMessage.contains("Password should be at least", ignoreCase = true) ->
                "Password must be at least 6 characters"
            rawMessage.contains("Invalid email", ignoreCase = true) ->
                "Please enter a valid email address"
            rawMessage.contains("duplicate key", ignoreCase = true) ->
                "Username already taken"
            rawMessage.contains("network", ignoreCase = true) ||
                    rawMessage.contains("connection", ignoreCase = true) ->
                "Network error. Please check your connection"
            rawMessage.contains("timeout", ignoreCase = true) ->
                "Request timed out. Please try again"
            else -> "Authentication failed. Please try again"
        }
    }
}
