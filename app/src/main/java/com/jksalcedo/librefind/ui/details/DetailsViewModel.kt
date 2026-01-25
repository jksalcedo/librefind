package com.jksalcedo.librefind.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.AuthRepository
import com.jksalcedo.librefind.domain.usecase.GetAlternativeUseCase
import com.jksalcedo.librefind.domain.usecase.ScanInventoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val getAlternativeUseCase: GetAlternativeUseCase,
    private val appRepository: AppRepository,
    private val authRepository: AuthRepository,
    private val scanInventoryUseCase: ScanInventoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()

    fun loadAlternatives(packageName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Check unknown status concurrently
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

    private suspend fun isAppUnknown(packageName: String) {
        val app = scanInventoryUseCase()
            .first()
            .find { it.packageName == packageName }
        
        _state.update { 
            it.copy(isUnknown = app?.status == AppStatus.UNKN) 
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
