package com.jksalcedo.librefind.ui.mysubmissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MySubmissionsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val submissions: List<Submission> = emptyList()
)

class MySubmissionsViewModel(
    private val appRepository: AppRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MySubmissionsUiState())
    val uiState: StateFlow<MySubmissionsUiState> = _uiState.asStateFlow()

    init {
        fetchSubmissions()
    }

    fun fetchSubmissions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "User not signed in"
                )
                return@launch
            }

            try {
                val submissions = appRepository.getUserSubmissions(user.uid)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    submissions = submissions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to fetch submissions"
                )
            }
        }
    }
}
