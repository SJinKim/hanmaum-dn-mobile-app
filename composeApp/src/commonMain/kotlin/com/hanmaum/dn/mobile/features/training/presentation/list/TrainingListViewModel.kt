package com.hanmaum.dn.mobile.features.training.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.training.domain.model.Training
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import com.hanmaum.dn.mobile.features.training.domain.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainingListUiState(
    val isLoading: Boolean = true,
    /** In the server's order. Never re-sorted here. */
    val trainings: List<Training> = emptyList(),
    val isUnavailable: Boolean = false,
    val hasError: Boolean = false,
)

class TrainingListViewModel(
    private val repository: TrainingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingListUiState())
    val uiState: StateFlow<TrainingListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, isUnavailable = false, hasError = false) }
        viewModelScope.launch {
            when (val result = repository.getTrainings()) {
                is TrainingResult.Success ->
                    _uiState.update { it.copy(isLoading = false, trainings = result.data) }
                TrainingResult.Unavailable ->
                    _uiState.update { it.copy(isLoading = false, trainings = emptyList(), isUnavailable = true) }
                TrainingResult.Failed ->
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }
}
