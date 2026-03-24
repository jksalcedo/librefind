package com.jksalcedo.librefind.ui.correction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SuggestCorrectionViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SuggestCorrectionState())
    val state: StateFlow<SuggestCorrectionState> = _state.asStateFlow()

    fun setPackageName(packageName: String) {
        _state.update { it.copy(packageName = packageName) }
    }

    fun onCorrectionTypeChanged(type: CorrectionType) {
        _state.update { it.copy(correctionType = type) }
    }

    fun onCorrectionValueChanged(value: String) {
        _state.update { it.copy(correctionValue = value) }
    }

    fun onDescriptionChanged(value: String) {
        _state.update { it.copy(description = value) }
    }

    fun submitCorrection() {
        val currentState = _state.value
        if (currentState.packageName.isBlank() ||
            currentState.correctionValue.isBlank() ||
            currentState.description.isBlank()
        ) {
            _state.update { it.copy(error = "Please fill in all fields") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = appRepository.submitCorrection(
                packageName = currentState.packageName,
                correctionType = currentState.correctionType.name,
                correctionValue = currentState.correctionValue,
                description = currentState.description
            )

            result.onSuccess {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to submit correction"
                    )
                }
            }
        }
    }
}

data class SuggestCorrectionState(
    val packageName: String = "",
    val correctionType: CorrectionType = CorrectionType.LICENSE,
    val correctionValue: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

enum class CorrectionType(val label: String) {
    LICENSE("License"),
    SOURCE_AVAILABLE("Source Available"),
    CATEGORY("Category"),
    LINKS("Website/Repo Links"),
    OTHER("Other")
}
