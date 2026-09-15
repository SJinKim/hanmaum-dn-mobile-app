package com.hanmaum.dn.mobile.features.training.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingCourse
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingDetail
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import com.hanmaum.dn.mobile.features.training.domain.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainingDetailUiState(
    val isLoading: Boolean = true,
    val detail: TrainingDetail? = null,
    val isUnavailable: Boolean = false,
    val hasError: Boolean = false,
    val selectedCourseId: Int? = null,
    val showCancelDialog: Boolean = false,
) {
    /** An application still running: the page shows 신청 현황 instead of offering to apply again. */
    val hasActiveApplication: Boolean
        get() = detail?.myApplication?.status?.isActive == true

    val selectedCourse: TrainingCourse?
        get() = detail?.courses?.firstOrNull { it.externalCourseId == selectedCourseId && it.isEligible }

    val canApply: Boolean
        get() = !hasActiveApplication && selectedCourse != null
}

class TrainingDetailViewModel(
    private val publicId: String,
    private val repository: TrainingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingDetailUiState())
    val uiState: StateFlow<TrainingDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, isUnavailable = false, hasError = false) }
        viewModelScope.launch {
            when (val result = repository.getTrainingDetail(publicId)) {
                is TrainingResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        detail = result.data,
                        selectedCourseId = preselectedCourseId(result.data),
                    )
                }
                TrainingResult.Unavailable ->
                    _uiState.update { it.copy(isLoading = false, detail = null, isUnavailable = true) }
                TrainingResult.Failed ->
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    /** A course this member may not apply to cannot be chosen, whatever the UI lets through. */
    fun selectCourse(externalCourseId: Int) {
        _uiState.update { state ->
            val course = state.detail?.courses?.firstOrNull { it.externalCourseId == externalCourseId }
            if (course?.isEligible == true) state.copy(selectedCourseId = externalCourseId) else state
        }
    }

    // Cancelling has no server endpoint yet (hanmaum-dn-server#167): the button only explains that.
    fun openCancelDialog() = _uiState.update { it.copy(showCancelDialog = true) }

    fun dismissCancelDialog() = _uiState.update { it.copy(showCancelDialog = false) }

    /** Exactly one open course is chosen for the member; several wait for an explicit choice. */
    private fun preselectedCourseId(detail: TrainingDetail): Int? =
        detail.courses.singleOrNull()?.takeIf { it.isEligible }?.externalCourseId
}
