package com.jksalcedo.fossia.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.fossia.domain.model.AppItem
import com.jksalcedo.fossia.domain.model.SovereigntyScore
import com.jksalcedo.fossia.domain.usecase.ScanInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Dashboard screen
 * 
 * Manages app scanning state and sovereignty score calculation
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scanInventoryUseCase: ScanInventoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                scanInventoryUseCase().collect { apps ->
                    val score = calculateScore(apps)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            apps = apps,
                            sovereigntyScore = score,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    private fun calculateScore(apps: List<AppItem>): SovereigntyScore {
        val totalApps = apps.size
        val fossCount = apps.count { it.status == com.jksalcedo.fossia.domain.model.AppStatus.FOSS }
        val propCount = apps.count { it.status == com.jksalcedo.fossia.domain.model.AppStatus.PROP }
        val unknownCount = apps.count { it.status == com.jksalcedo.fossia.domain.model.AppStatus.UNKN }

        return SovereigntyScore(
            totalApps = totalApps,
            fossCount = fossCount,
            proprietaryCount = propCount,
            unknownCount = unknownCount
        )
    }
}

/**
 * UI State for Dashboard screen
 */
data class DashboardState(
    val isLoading: Boolean = false,
    val apps: List<AppItem> = emptyList(),
    val sovereigntyScore: SovereigntyScore? = null,
    val error: String? = null
)
