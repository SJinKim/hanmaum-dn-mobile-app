package com.hanmaum.dn.mobile.features.training.presentation.myapplications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.training.domain.model.CancelResult
import com.hanmaum.dn.mobile.features.training.domain.model.MyApplication
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import com.hanmaum.dn.mobile.features.training.domain.repository.TrainingRepository
import com.hanmaum.dn.mobile.features.training.presentation.detail.CancelOutcome
import com.hanmaum.dn.mobile.features.training.presentation.detail.CancelPrompt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 나의 신청 — the 양육 half only.
 *
 * The 사역 half has no server behind it yet (hanmaum-dn-server#170), so it is a placeholder the
 * screen draws without asking anyone; nothing about it belongs in this state.
 */
data class MyApplicationsUiState(
    val isLoading: Boolean = true,
    /** In the server's order — newest first. Never re-sorted here. */
    val applications: List<MyApplication> = emptyList(),
    val isUnavailable: Boolean = false,
    val hasError: Boolean = false,
    /** The 신청 취소 dialog; null while it is closed. */
    val cancelPrompt: CancelPrompt? = null,
    /**
     * Which application [cancelPrompt] is about. The detail page could leave this implicit —
     * it only ever shows one — but a list cannot.
     */
    val cancelTarget: String? = null,
)

class MyApplicationsViewModel(
    private val repository: TrainingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyApplicationsUiState())
    val uiState: StateFlow<MyApplicationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, isUnavailable = false, hasError = false) }
        viewModelScope.launch {
            when (val result = repository.getMyApplications()) {
                is TrainingResult.Success ->
                    _uiState.update { it.copy(isLoading = false, applications = result.data) }
                TrainingResult.Unavailable ->
                    _uiState.update { it.copy(isLoading = false, applications = emptyList(), isUnavailable = true) }
                TrainingResult.Failed ->
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    /** Only a running application can be cancelled, whatever the UI lets through. */
    fun openCancelDialog(application: MyApplication) = _uiState.update { state ->
        if (application.status.isActive) {
            state.copy(
                cancelPrompt = CancelPrompt(application.status),
                cancelTarget = application.trainingPublicId,
            )
        } else {
            state
        }
    }

    /**
     * Closes the dialog, unless the call is still running. After 404 the list reloads: the
     * application the member was looking at is gone, and leaving the row would be a lie.
     */
    fun dismissCancelDialog() {
        val prompt = _uiState.value.cancelPrompt ?: return
        if (!prompt.isDismissable) return
        _uiState.update { it.copy(cancelPrompt = null, cancelTarget = null) }
        if (prompt.outcome is CancelOutcome.NotFound) load()
    }

    fun confirmCancel() {
        val prompt = _uiState.value.cancelPrompt ?: return
        val target = _uiState.value.cancelTarget ?: return
        if (!prompt.canConfirm) return

        // Set before launching, so a second tap in the same frame already finds it running.
        updatePrompt { it.copy(isCancelling = true, outcome = null) }
        viewModelScope.launch {
            when (repository.cancel(target)) {
                is CancelResult.Success -> {
                    // The server sends the cancelled application back, but the row's status and
                    // whether it still offers 신청 취소 follow the whole list, so it is reloaded.
                    _uiState.update { it.copy(cancelPrompt = null, cancelTarget = null) }
                    load()
                }
                CancelResult.NotCancellable -> updatePrompt {
                    it.copy(isCancelling = false, outcome = CancelOutcome.NotCancellable)
                }
                CancelResult.NotFound -> updatePrompt {
                    it.copy(isCancelling = false, outcome = CancelOutcome.NotFound)
                }
                CancelResult.Unavailable -> updatePrompt {
                    it.copy(isCancelling = false, outcome = CancelOutcome.Unavailable)
                }
                CancelResult.Failed -> updatePrompt {
                    it.copy(isCancelling = false, outcome = CancelOutcome.Failed)
                }
            }
        }
    }

    private fun updatePrompt(block: (CancelPrompt) -> CancelPrompt) =
        _uiState.update { state -> state.cancelPrompt?.let { state.copy(cancelPrompt = block(it)) } ?: state }
}
