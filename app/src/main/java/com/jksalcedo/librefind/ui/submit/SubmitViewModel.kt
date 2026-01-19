package com.jksalcedo.librefind.ui.submit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.remote.firebase.AuthService
import com.jksalcedo.librefind.data.remote.firebase.FirestoreService
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.model.SubmissionType
import com.jksalcedo.librefind.domain.model.SubmittedApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubmitUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val proprietaryTargets: List<String> = emptyList()
)

class SubmitViewModel(
    private val authService: AuthService,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmitUiState())
    val uiState: StateFlow<SubmitUiState> = _uiState.asStateFlow()

    init {
        loadProprietaryTargets()
    }

    private fun loadProprietaryTargets() {
        viewModelScope.launch {
            val targets = firestoreService.getProprietaryTargets()
            _uiState.value = _uiState.value.copy(proprietaryTargets = targets)
        }
    }

    fun submit(
        type: SubmissionType,
        appName: String,
        packageName: String,
        description: String,
        repoUrl: String = "",
        fdroidId: String = "",
        license: String = "",
        proprietaryPackages: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val user = authService.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }

            val profile = firestoreService.getProfile(user.uid)
            if (profile == null) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Profile not set up")
                return@launch
            }

            val submission = Submission(
                type = type,
                proprietaryPackages = proprietaryPackages,
                submitterUid = user.uid,
                submitterUsername = profile.username,
                submittedApp = SubmittedApp(
                    name = appName,
                    packageName = packageName,
                    repoUrl = repoUrl,
                    fdroidId = fdroidId,
                    description = description,
                    license = license
                )
            )

            firestoreService.submitEntry(submission)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, success = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Submission failed"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = SubmitUiState(proprietaryTargets = _uiState.value.proprietaryTargets)
    }
}
