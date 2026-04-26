package com.jksalcedo.librefind.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunitySubmissionsState(
    val submissions: List<Submission> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CommunitySubmissionsViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunitySubmissionsState())
    val uiState: StateFlow<CommunitySubmissionsState> = _uiState.asStateFlow()

    init {
        loadSubmissions()
    }

    fun loadSubmissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val submissions = appRepository.getAllPendingSubmissions()
                _uiState.update { it.copy(submissions = submissions, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load submissions") }
            }
        }
    }

    fun approveSubmission(submission: Submission) {
        viewModelScope.launch {
            appRepository.approveSubmission(submission.id, submission.type)
                .onSuccess {
                    loadSubmissions()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to approve submission") }
                }
        }
    }

    fun rejectSubmission(submission: Submission, reason: String) {
        viewModelScope.launch {
            appRepository.rejectSubmission(submission.id, submission.type, reason)
                .onSuccess {
                    loadSubmissions()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to reject submission") }
                }
        }
    }
}
