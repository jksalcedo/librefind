package com.jksalcedo.librefind.ui.submit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.SubmissionType
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.AuthRepository
import com.jksalcedo.librefind.domain.usecase.ScanInventoryUseCase
import com.jksalcedo.librefind.domain.usecase.SubmitProposalUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SubmitUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val proprietaryTargets: List<String> = emptyList(),
    val unknownApps: Map<String, String> = emptyMap(),
    val duplicateWarning: String? = null,
    val packageNameError: String? = null,
    val repoUrlError: String? = null,
    val submittedAppName: String? = null,
    val solutionSearchResults: List<com.jksalcedo.librefind.domain.model.Alternative> = emptyList(),
    val selectedAlternatives: Set<String> = emptySet(),
    val isEditing: Boolean = false,
    val editingSubmissionId: String? = null,
    val loadedSubmission: com.jksalcedo.librefind.domain.model.Submission? = null
)

class SubmitViewModel(
    private val authRepository: AuthRepository,
    private val appRepository: AppRepository,
    private val submitProposalUseCase: SubmitProposalUseCase,
    private val updateSubmissionUseCase: com.jksalcedo.librefind.domain.usecase.UpdateSubmissionUseCase,
    private val scanInventoryUseCase: ScanInventoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmitUiState())
    val uiState: StateFlow<SubmitUiState> = _uiState.asStateFlow()

    init {
        loadProprietaryTargets()
        loadUnknownApps()
    }

    private fun loadProprietaryTargets() {
        viewModelScope.launch {
            val targets = appRepository.getProprietaryTargets()
            _uiState.value = _uiState.value.copy(proprietaryTargets = targets)
        }
    }

    private fun loadUnknownApps() {
        viewModelScope.launch {
            val apps = scanInventoryUseCase()
                .first()
                .filter { it.status == com.jksalcedo.librefind.domain.model.AppStatus.UNKN }
                .associate { it.packageName to it.label }
            _uiState.value = _uiState.value.copy(unknownApps = apps)
        }
    }

    fun loadSubmission(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val user = authRepository.getCurrentUser() ?: return@launch
            val submissions = appRepository.getMySubmissions(user.uid)
            val submission = submissions.find { it.id == id }

            if (submission != null) {
                // Pre-fill state
                val isProprietary = submission.type == SubmissionType.NEW_PROPRIETARY
                
                // If it's a proprietary submission, we need to load the alternatives
                // But the current Submission model doesn't have alternatives list easily accessible 
                // without fetching details. For now, we'll just load basic info.
                // NOTE: A better approach would be to fetch full submission details by ID.
                // Assuming we can get by with what we have or fetch more if needed.
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isEditing = true,
                    editingSubmissionId = id,
                    loadedSubmission = submission
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Submission not found")
            }
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

            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }

            if (_uiState.value.packageNameError != null || _uiState.value.repoUrlError != null) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Please fix validation errors")
                return@launch
            }

            if (user.username.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Profile not set up")
                return@launch
            }

            val result = if (_uiState.value.isEditing && _uiState.value.editingSubmissionId != null) {
                updateSubmissionUseCase(
                    id = _uiState.value.editingSubmissionId!!,
                    proprietaryPackage = proprietaryPackages,
                    alternativeId = packageName,
                    appName = appName,
                    description = description,
                    repoUrl = repoUrl,
                    fdroidId = fdroidId,
                    license = license,
                    alternatives = _uiState.value.selectedAlternatives.toList()
                )
            } else {
                submitProposalUseCase(
                    proprietaryPackage = proprietaryPackages,
                    alternativeId = packageName,
                    appName = appName,
                    description = description,
                    repoUrl = repoUrl,
                    fdroidId = fdroidId,
                    license = license,
                    userId = user.uid,
                    alternatives = _uiState.value.selectedAlternatives.toList()
                )
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = true,
                    submittedAppName = appName
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Submission failed: ${e.message}"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = SubmitUiState(
            proprietaryTargets = _uiState.value.proprietaryTargets,
            unknownApps = _uiState.value.unknownApps,
            isEditing = false,
            editingSubmissionId = null,
            loadedSubmission = null
        )
    }

    private var checkDuplicateJob: kotlinx.coroutines.Job? = null

    fun checkDuplicate(name: String, packageName: String) {
        checkDuplicateJob?.cancel()
        checkDuplicateJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            if (name.isBlank() && packageName.isBlank()) {
                _uiState.value = _uiState.value.copy(duplicateWarning = null)
                return@launch
            }

            val isDuplicate = appRepository.checkDuplicateApp(name, packageName)
            val warning = if (isDuplicate) {
                "This app is already in our database."
            } else {
                null
            }
            _uiState.value = _uiState.value.copy(duplicateWarning = warning)
        }
    }

    fun validatePackageName(packageName: String) {
        // Regex: ^[a-z][a-z0-9_]*(\.[a-z0-9_]+)+[0-9a-z_]$
        // Starts with a letter
        // Contains lowercase letters, numbers, underscores
        // Must have at least one dot separating parts
        // Parts must start with letter/number/underscore (regex says [a-z0-9_]+ so yes)
        // Ends with letter/number/underscore
        val regex = Regex("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$")
        val isValid = regex.matches(packageName)

        _uiState.value = _uiState.value.copy(
            packageNameError = if (isValid) null else "Invalid package name format (e.g. com.example.app)"
        )
    }

    fun validateRepoUrl(url: String) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(repoUrlError = null)
            return
        }

        val isValid = url.startsWith("https://")
        _uiState.value = _uiState.value.copy(
            repoUrlError = if (isValid) null else "URL must start with https://"
        )
    }

    private var searchSolutionsJob: Job? = null

    fun searchSolutions(query: String) {
        searchSolutionsJob?.cancel()
        
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(solutionSearchResults = emptyList())
            return
        }
        
        searchSolutionsJob = viewModelScope.launch {
            delay(300)
            try {
                val results = appRepository.searchSolutions(query, limit = 10)
                _uiState.value = _uiState.value.copy(solutionSearchResults = results)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(solutionSearchResults = emptyList())
            }
        }
    }

    fun addAlternative(packageName: String) {
        val current = _uiState.value.selectedAlternatives
        _uiState.value = _uiState.value.copy(selectedAlternatives = current + packageName)
    }

    fun removeAlternative(packageName: String) {
        val current = _uiState.value.selectedAlternatives
        _uiState.value = _uiState.value.copy(selectedAlternatives = current - packageName)
    }

    fun clearSolutionSearchResults() {
        _uiState.value = _uiState.value.copy(solutionSearchResults = emptyList())
    }
}
