package com.jksalcedo.librefind.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.AuthRepository
import com.jksalcedo.librefind.domain.repository.CacheRepository
import com.jksalcedo.librefind.domain.usecase.GetAlternativeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val getAlternativeUseCase: GetAlternativeUseCase,
    private val authRepository: AuthRepository,
    private val appRepository: AppRepository,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()

    fun loadAlternatives(packageName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Check unknown status using lightweight cache lookups
            launch {
                isAppUnknown(packageName)
            }

            try {
                val alternatives = getAlternativeUseCase(packageName)
                val user = authRepository.getCurrentUser()

                _state.update {
                    it.copy(
                        isLoading = false,
                        packageName = packageName,
                        alternatives = alternatives,
                        isSignedIn = user != null,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load alternatives"
                    )
                }
            }
        }
    }

    fun rateAlternative(altId: String, stars: Int) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                appRepository.castVote(altId, "usability", stars)
                _state.value.packageName.let { pkg ->
                    if (pkg.isNotEmpty()) {
                        loadAlternatives(pkg)
                    }
                }
            }
        }
    }

    fun retry(packageName: String) {
        loadAlternatives(packageName)
    }

    /**
     * Lightweight check using cached data instead of a full device scan.
     * An app is "unknown" if it's neither a known proprietary target nor a known FOSS solution.
     */
    private suspend fun isAppUnknown(packageName: String) {
        val isTarget = try {
            cacheRepository.isTargetCached(packageName) || appRepository.isProprietary(packageName)
        } catch (_: Exception) {
            false
        }

        val isSolution = try {
            cacheRepository.isSolutionCached(packageName) || appRepository.isSolution(packageName)
        } catch (_: Exception) {
            false
        }

        val isUnknown = !isTarget && !isSolution

        _state.update {
            it.copy(isUnknown = isUnknown)
        }
    }
}

data class DetailsState(
    val isLoading: Boolean = false,
    val packageName: String = "",
    val alternatives: List<Alternative> = emptyList(),
    val isSignedIn: Boolean = false,
    val error: String? = null,
    val isUnknown: Boolean = false
)