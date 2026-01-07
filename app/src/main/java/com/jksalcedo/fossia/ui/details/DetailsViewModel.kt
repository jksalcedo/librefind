package com.jksalcedo.fossia.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.fossia.domain.model.Alternative
import com.jksalcedo.fossia.domain.usecase.GetAlternativeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Details screen
 * 
 * Manages fetching and displaying alternatives for a proprietary app
 */
@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getAlternativeUseCase: GetAlternativeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>("packageName") ?: ""

    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()

    init {
        loadAlternatives()
    }

    private fun loadAlternatives() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val alternatives = getAlternativeUseCase(packageName)
                _state.update {
                    it.copy(
                        isLoading = false,
                        packageName = packageName,
                        alternatives = alternatives,
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

    fun retry() {
        loadAlternatives()
    }
}

/**
 * UI State for Details screen
 */
data class DetailsState(
    val isLoading: Boolean = false,
    val packageName: String = "",
    val alternatives: List<Alternative> = emptyList(),
    val error: String? = null
)
