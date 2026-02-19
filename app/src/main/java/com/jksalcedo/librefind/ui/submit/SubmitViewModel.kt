package com.jksalcedo.librefind.ui.submit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.model.SubmissionType
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.AuthRepository
import com.jksalcedo.librefind.domain.repository.CacheRepository
import com.jksalcedo.librefind.domain.usecase.SubmitProposalUseCase
import com.jksalcedo.librefind.domain.usecase.UpdateSubmissionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val solutionSearchResults: List<Alternative> = emptyList(),
    val selectedAlternatives: Set<String> = emptySet(),
    val isEditing: Boolean = false,
    val editingSubmissionId: String? = null,
    val loadedSubmission: Submission? = null,
    // FOSS Search
    val fossSearchResults: List<Alternative> = emptyList(),
    val linkedSolution: Alternative? = null,
    val linkTargetPackage: String? = null
)

class SubmitViewModel(
    private val authRepository: AuthRepository,
    private val appRepository: AppRepository,
    private val submitProposalUseCase: SubmitProposalUseCase,
    private val updateSubmissionUseCase: UpdateSubmissionUseCase,
    private val cacheRepository: CacheRepository,
    private val inventorySource: InventorySource
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
            try {
                // Use lightweight local-only check instead of full scan + network classification
                val rawApps = withContext(Dispatchers.IO) {
                    inventorySource.getRawApps()
                }

                val unknownApps = mutableMapOf<String, String>()

                for (pkg in rawApps) {
                    val packageName = pkg.packageName
                    val isTarget = try {
                        cacheRepository.isTargetCached(packageName)
                    } catch (_: Exception) {
                        false
                    }

                    val isSolution = try {
                        cacheRepository.isSolutionCached(packageName)
                    } catch (_: Exception) {
                        false
                    }

                    if (!isTarget && !isSolution) {
                        val label = withContext(Dispatchers.IO) {
                            inventorySource.getAppLabel(packageName)
                        }
                        unknownApps[packageName] = label
                    }
                }

                _uiState.value = _uiState.value.copy(unknownApps = unknownApps)
            } catch (e: Exception) {
                Log.w("SubmitVM", "Failed to load unknown apps", e)
                _uiState.value = _uiState.value.copy(unknownApps = emptyMap())
            }
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
                //val isProprietary = submission.type == SubmissionType.NEW_PROPRIETARY

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isEditing = true,
                    editingSubmissionId = id,
                    loadedSubmission = submission
                )
            } else {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Submission not found")
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
        proprietaryPackages: String = "",
        category: String = ""
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

            if (_uiState.value.duplicateWarning != null) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Duplicate submission")
                return@launch
            }

            val result =
                if (type == SubmissionType.LINKING) {
                    if (_uiState.value.linkTargetPackage == null) {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                error = "Please select a target app"
                            )
                        return@launch
                    }
                    if (_uiState.value.selectedAlternatives.isEmpty()) {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                error = "Please select at least one solution"
                            )
                        return@launch
                    }

                    appRepository.submitLinkedAlternatives(
                        proprietaryPackage = _uiState.value.linkTargetPackage!!,
                        alternatives = _uiState.value.selectedAlternatives.toList(),
                        submitterId = user.uid
                    )

                } else if (_uiState.value.isEditing && _uiState.value.editingSubmissionId != null) {
                    updateSubmissionUseCase(
                        id = _uiState.value.editingSubmissionId!!,
                        proprietaryPackage = proprietaryPackages,
                        alternativeId = packageName,
                        appName = appName,
                        description = description,
                        repoUrl = repoUrl,
                        fdroidId = fdroidId,
                        license = license,
                        alternatives = _uiState.value.selectedAlternatives.toList(),
                        category = category
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
                        alternatives = _uiState.value.selectedAlternatives.toList(),
                        submissionType = type,
                        category = category
                    )
                }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = true,
                    submittedAppName = if (type == SubmissionType.LINKING) _uiState.value.linkTargetPackage else appName
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Submission failed: ${e.message}"
                )
            }
        }
    }

    fun setLinkTarget(packageName: String) {
        _uiState.value = _uiState.value.copy(linkTargetPackage = packageName)
    }

    fun resetState() {
        _uiState.value = SubmitUiState(
            proprietaryTargets = _uiState.value.proprietaryTargets,
            unknownApps = _uiState.value.unknownApps,
            isEditing = false,
            editingSubmissionId = null,
            loadedSubmission = null,
            linkedSolution = null,
            fossSearchResults = emptyList()
        )
    }

    private var checkDuplicateJob: Job? = null

    private var searchFossAppsJob: Job? = null

    fun searchFossApps(query: String) {
        searchFossAppsJob?.cancel()

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(fossSearchResults = emptyList())
            return
        }

        searchFossAppsJob = viewModelScope.launch {
            delay(300)
            try {
                val results = appRepository.searchSolutions(query, limit = 10)
                _uiState.value = _uiState.value.copy(fossSearchResults = results)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(fossSearchResults = emptyList())
            }
        }
    }

    fun selectFossApp(app: Alternative) {
        _uiState.value = _uiState.value.copy(
            linkedSolution = app,
            fossSearchResults = emptyList(), // Clear results to hide dropdown
            duplicateWarning = null // Clear any duplicate warning as this is a known existing app
        )
    }

    fun clearLinkedApp() {
        _uiState.value = _uiState.value.copy(
            linkedSolution = null,
            duplicateWarning = null
        )
    }

    fun checkDuplicate(packageName: String) {
        checkDuplicateJob?.cancel()

        // If this package matches the linked solution, we skip the duplicate check
        // because we WANT to allow linking to an existing app.
        if (_uiState.value.linkedSolution?.packageName == packageName) {
            _uiState.value = _uiState.value.copy(duplicateWarning = null)
            return
        }

        checkDuplicateJob = viewModelScope.launch {
            delay(500)
            if (packageName.isBlank()) {
                _uiState.value = _uiState.value.copy(duplicateWarning = null)
                return@launch
            }

            val isDuplicate = appRepository.checkDuplicateApp(packageName)
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

        val error = when {
            !url.startsWith("https://") ->
                "URL must start with https://"

            !isAllowedFossHost(url) ->
                "URL must be from a known code hosting platform (e.g., GitHub, GitLab, Codeberg, or a self-hosted Git instance)"

            url.count { it == '/' } < 4 ->
                "URL must include the repository path (e.g. https://github.com/owner/repo)"

            else -> null
        }

        _uiState.value = _uiState.value.copy(repoUrlError = error)
    }

    private fun isAllowedFossHost(urlString: String): Boolean {
        val host = try {
            java.net.URL(urlString).host?.lowercase() ?: return false
        } catch (_: Exception) {
            return false
        }

        val allowedSuffixes = listOf(
            "github.com", "gitlab.com", "bitbucket.org", "codeberg.org",
            "sr.ht", "gitea.com", "framagit.org", "notabug.org",
            "kde.org", "gnome.org", "debian.org", "gnu.org",
            "wikimedia.org", "freedesktop.org", "torproject.org",
            "kernel.org", "videolan.org"
        )

        // Check if the host matches exactly or is a subdomain (e.g., gist.github.com)
        if (allowedSuffixes.any { host == it || host.endsWith(".$it") }) {
            return true
        }

        // Catch-all for unknown, self-hosted FOSS instances using common subdomains
        if (host.startsWith("git.") || host.startsWith("gitlab.") || host.startsWith("gitea.") || host.startsWith(
                "forgejo."
            )
        ) {
            return true
        }

        return false
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
