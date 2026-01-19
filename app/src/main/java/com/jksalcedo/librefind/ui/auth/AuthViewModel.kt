package com.jksalcedo.librefind.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.remote.firebase.AuthService
import com.jksalcedo.librefind.data.remote.firebase.FirestoreService
import com.jksalcedo.librefind.domain.model.UserProfile
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
    val userProfile: UserProfile? = null
)

class AuthViewModel(
    private val authService: AuthService,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                if (user != null) {
                    val profile = firestoreService.getProfile(user.uid)
                    _uiState.value = _uiState.value.copy(
                        isSignedIn = true,
                        needsProfileSetup = profile == null,
                        profileComplete = profile != null,
                        userProfile = profile
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

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authService.signUp(email, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSignedIn = true,
                        needsProfileSetup = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Sign up failed"
                    )
                }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authService.signIn(email, password)
                .onSuccess { user ->
                    val profile = firestoreService.getProfile(user.uid)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSignedIn = true,
                        needsProfileSetup = profile == null,
                        profileComplete = profile != null,
                        userProfile = profile
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Sign in failed"
                    )
                }
        }
    }

    fun saveProfile(username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val user = authService.getCurrentUser() ?: return@launch

            if (firestoreService.isUsernameTaken(username)) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Username is already taken"
                )
                return@launch
            }

            val profile = UserProfile(
                uid = user.uid,
                username = username,
                email = user.email ?: ""
            )

            if (firestoreService.createOrUpdateProfile(profile)) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    needsProfileSetup = false,
                    profileComplete = true,
                    userProfile = profile
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save profile"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
