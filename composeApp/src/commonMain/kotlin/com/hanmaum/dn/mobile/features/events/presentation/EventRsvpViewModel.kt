package com.hanmaum.dn.mobile.features.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.EventRsvpPreferences
import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.repository.EventRsvpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventRsvpViewModel(
    private val repository: EventRsvpRepository,
    private val preferences: EventRsvpPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventRsvpUiState())
    val uiState: StateFlow<EventRsvpUiState> = _uiState.asStateFlow()

    /** Loads active RSVPs and shows the sheet for any the member has not yet handled. */
    fun refresh() {
        viewModelScope.launch {
            repository.getActiveRsvps().fold(
                onSuccess = { list ->
                    val pending = list.filterNot { preferences.isHandled(it.publicId) }
                    _uiState.update {
                        it.copy(
                            events = pending,
                            visible = pending.isNotEmpty(),
                            checkingInId = null,
                            rowErrors = emptyMap(),
                        )
                    }
                },
                onFailure = { err ->
                    // Non-critical prompt: never block Home on a network error.
                    println("[EventRsvpViewModel] active load failed: ${err.message}")
                },
            )
        }
    }

    fun checkIn(publicId: String) {
        val current = _uiState.value
        if (current.checkingInId != null || publicId in current.checkedInIds) return
        _uiState.update { it.copy(checkingInId = publicId, rowErrors = it.rowErrors - publicId) }
        viewModelScope.launch {
            when (repository.checkIn(publicId)) {
                is CheckInResult.Success, CheckInResult.AlreadyRegistered -> {
                    preferences.markHandled(publicId)
                    _uiState.update { it.copy(checkingInId = null, checkedInIds = it.checkedInIds + publicId) }
                }
                CheckInResult.WindowClosed -> {
                    _uiState.update {
                        it.copy(checkingInId = null, rowErrors = it.rowErrors + (publicId to WINDOW_CLOSED_MSG))
                    }
                    refresh()
                }
                CheckInResult.Failed -> {
                    _uiState.update {
                        it.copy(checkingInId = null, rowErrors = it.rowErrors + (publicId to FAILED_MSG))
                    }
                }
            }
        }
    }

    fun dismiss(publicId: String) {
        preferences.markHandled(publicId)
        _uiState.update {
            val remaining = it.events.filterNot { e -> e.publicId == publicId }
            it.copy(events = remaining, visible = remaining.isNotEmpty())
        }
    }

    fun dismissAll() {
        _uiState.value.events.forEach { preferences.markHandled(it.publicId) }
        _uiState.update { it.copy(events = emptyList(), visible = false) }
    }

    private companion object {
        const val WINDOW_CLOSED_MSG = "지금은 참석 가능하지 않습니다. 나중에 다시 시도해주세요."
        const val FAILED_MSG = "참석 처리에 실패했습니다"
    }
}
