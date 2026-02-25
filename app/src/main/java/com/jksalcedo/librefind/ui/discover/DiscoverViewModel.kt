package com.jksalcedo.librefind.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val fossResults: List<Alternative> = emptyList(),
    val proprietaryResults: List<Alternative> = emptyList(),
    val isProprietaryTabSelected: Boolean = false,
    val error: String? = null
)

class DiscoverViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(fossResults = emptyList(), proprietaryResults = emptyList(), isLoading = false, error = null) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            delay(500) // Debounce typing
            try {
                val fossResults = repository.searchSolutions(query)
                val propResults = repository.searchProprietary(query)
                _uiState.update { it.copy(fossResults = fossResults, proprietaryResults = propResults, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    fun setProprietaryTabSelected(isSelected: Boolean) {
        _uiState.update { it.copy(isProprietaryTabSelected = isSelected) }
    }
}
