package com.hanmaum.dn.mobile.features.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.EventRsvpPreferences
import com.hanmaum.dn.mobile.features.events.domain.model.RespondResult
import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus
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

    /**
     * Loads every open RSVP and decides whether the sheet should appear.
     *
     * The sheet offers only genuinely unanswered events that have not been put
     * off. A MAYBE has been answered — asking again on every launch would be
     * nagging, and the server's reminder covers that case instead.
     */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getActiveRsvps().fold(
                onSuccess = { list ->
                    val offerSheet = list.any {
                        it.myStatus == null && !preferences.isHandled(it.publicId)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            events = list,
                            visible = offerSheet,
                            respondingTo = null,
                            rowErrors = emptyMap(),
                        )
                    }
                },
                onFailure = { err ->
                    // The sheet is a prompt, not a screen: never block Home on it.
                    _uiState.update {
                        it.copy(isLoading = false, error = err.message ?: LOAD_FAILED_MSG)
                    }
                },
            )
        }
    }

    /**
     * Applies the answer straight away and reverts if the server refuses.
     *
     * Optimistic because the tap must feel immediate, and safe because the
     * endpoint is idempotent — a retry after a dropped connection cannot create
     * a second answer.
     */
    fun respond(publicId: String, status: RsvpStatus) {
        val current = _uiState.value
        if (current.respondingTo != null) return
        val previous = current.events.firstOrNull { it.publicId == publicId } ?: return

        _uiState.update { state ->
            state.copy(
                respondingTo = publicId,
                rowErrors = state.rowErrors - publicId,
                events = state.events.map {
                    if (it.publicId == publicId) it.copy(myStatus = status) else it
                },
            )
        }

        viewModelScope.launch {
            when (val result = repository.respond(publicId, status)) {
                is RespondResult.Success -> {
                    // Answered means the sheet has no more business with it.
                    preferences.markHandled(publicId)
                    _uiState.update { state ->
                        state.copy(
                            respondingTo = null,
                            events = state.events.map {
                                if (it.publicId == publicId) it.copy(myStatus = result.status) else it
                            },
                        )
                    }
                }
                RespondResult.WindowClosed -> revert(publicId, previous, WINDOW_CLOSED_MSG)
                RespondResult.Failed -> revert(publicId, previous, FAILED_MSG)
            }
        }
    }

    private fun revert(
        publicId: String,
        previous: com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp,
        message: String,
    ) {
        _uiState.update { state ->
            state.copy(
                respondingTo = null,
                rowErrors = state.rowErrors + (publicId to message),
                events = state.events.map { if (it.publicId == publicId) previous else it },
            )
        }
    }

    /**
     * "나중에" — a deferral, not an answer.
     *
     * Nothing is sent to the server; the event stays pending and is answered
     * later from the RSVP screen. The choice is remembered across launches so
     * the sheet does not reappear on every cold start, which is what made the
     * old prompt feel like nagging.
     */
    fun dismissSheet() {
        _uiState.value.events
            .filter { it.myStatus == null }
            .forEach { preferences.markHandled(it.publicId) }
        _uiState.update { it.copy(visible = false) }
    }

    fun clearRowError(publicId: String) {
        _uiState.update { it.copy(rowErrors = it.rowErrors - publicId) }
    }

    private companion object {
        const val WINDOW_CLOSED_MSG = "응답 기간이 지났습니다"
        const val FAILED_MSG = "응답을 저장하지 못했습니다"
        const val LOAD_FAILED_MSG = "행사 목록을 불러오지 못했습니다"
    }
}
