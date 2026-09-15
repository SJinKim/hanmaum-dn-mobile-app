package com.hanmaum.dn.mobile.features.training.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicationField
import com.hanmaum.dn.mobile.features.training.domain.model.ApplyResult
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
    /** The application sheet; null while it is closed. */
    val form: ApplicationFormState? = null,
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

    // ─── Application form (#174) ─────────────────────────────────────────────

    fun openApplicationForm() {
        _uiState.update { state ->
            val course = state.selectedCourse
            if (!state.canApply || course == null) {
                state
            } else {
                state.copy(form = ApplicationForms.open(course, state.detail?.applicantPrefill))
            }
        }
    }

    /**
     * Editing a field clears its error; the rest stay until the next attempt. A locked field
     * (a profile value) ignores edits, whatever the UI lets through.
     */
    fun updateField(field: ApplicationField, value: String) = updateForm {
        if (it.isLocked(field)) it else it.copy(values = it.values + (field to value), errors = it.errors - field)
    }

    fun setConsent(checked: Boolean) = updateForm { it.copy(consent = checked) }

    fun submitApplication() {
        val form = _uiState.value.form ?: return
        if (!form.canSubmit) return

        val errors = ApplicationForms.validate(form)
        if (errors.isNotEmpty()) {
            updateForm { it.copy(errors = errors, outcome = null) }
            return
        }

        // Set before launching, so a second tap in the same frame already finds it sending.
        updateForm { it.copy(isSubmitting = true, errors = emptyMap(), outcome = null) }
        viewModelScope.launch {
            val result = repository.apply(publicId, form.course.externalCourseId, ApplicationForms.requestValues(form))
            val stillOpen = _uiState.value.form?.course?.externalCourseId == form.course.externalCourseId
            when {
                stillOpen -> updateForm { it.after(result) }
                // The sheet was closed while sending: an application made meanwhile must still
                // show up on the page.
                result.recordsAnApplication() -> load()
            }
        }
    }

    /**
     * Closes the sheet. After an application was made, or found to exist already, the page
     * reloads so that 신청 현황 shows it.
     */
    fun closeApplicationForm() {
        val outcome = _uiState.value.form?.outcome
        _uiState.update { it.copy(form = null) }
        if (outcome is FormOutcome.Success || outcome is FormOutcome.AlreadyApplied) load()
    }

    private fun updateForm(transform: (ApplicationFormState) -> ApplicationFormState) {
        _uiState.update { state -> state.form?.let { state.copy(form = transform(it)) } ?: state }
    }

    /** Exactly one open course is chosen for the member; several wait for an explicit choice. */
    private fun preselectedCourseId(detail: TrainingDetail): Int? =
        detail.courses.singleOrNull()?.takeIf { it.isEligible }?.externalCourseId
}

private fun ApplyResult.recordsAnApplication(): Boolean =
    this is ApplyResult.Success || this is ApplyResult.AlreadyApplied

private fun ApplicationFormState.after(result: ApplyResult): ApplicationFormState {
    val done = copy(isSubmitting = false)
    return when (result) {
        is ApplyResult.Success -> done.copy(outcome = FormOutcome.Success(result.courseName))
        ApplyResult.Unavailable -> done.copy(outcome = FormOutcome.Unavailable)
        ApplyResult.Full -> done.copy(outcome = FormOutcome.Full)
        ApplyResult.Closed -> done.copy(outcome = FormOutcome.Closed)
        ApplyResult.AlreadyApplied -> done.copy(outcome = FormOutcome.AlreadyApplied)
        is ApplyResult.NotEligible -> done.copy(outcome = FormOutcome.NotEligible(result.message))
        is ApplyResult.Invalid -> done.copy(
            errors = result.fieldErrors.mapValues { (_, message) -> FieldError.Server(message) },
            outcome = FormOutcome.Invalid(result.message),
        )
        ApplyResult.Failed -> done.copy(outcome = FormOutcome.Failed)
    }
}
