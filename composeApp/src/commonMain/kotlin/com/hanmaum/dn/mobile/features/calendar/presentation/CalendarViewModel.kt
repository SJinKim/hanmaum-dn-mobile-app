package com.hanmaum.dn.mobile.features.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent
import com.hanmaum.dn.mobile.features.calendar.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(run {
        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        CalendarUiState(
            year = now.year,
            month = now.monthNumber,
            todayYear = now.year,
            todayMonth = now.monthNumber,
        )
    })
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    /**
     * Loads (or reloads) the currently-viewed month — and the year list when the
     * list view is active. Driven by the screen on every entry so events created
     * in the web app appear without a re-login.
     */
    fun refresh() {
        loadCurrentMonth()
        if (_uiState.value.viewMode == ViewMode.LIST) loadYearEvents()
    }

    fun selectDay(day: Int) {
        _uiState.update { it.copy(selectedDay = if (it.selectedDay == day) null else day) }
    }

    fun selectEvent(event: CalendarEvent) {
        _uiState.update { it.copy(selectedEvent = event) }
    }

    fun dismissEventDetail() {
        _uiState.update { it.copy(selectedEvent = null) }
    }

    fun previousMonth() {
        _uiState.update { s ->
            val (y, m) = if (s.month == 1) s.year - 1 to 12 else s.year to s.month - 1
            s.copy(year = y, month = m, selectedDay = null, selectedEvent = null)
        }
        loadCurrentMonth()
    }

    fun nextMonth() {
        _uiState.update { s ->
            val (y, m) = if (s.month == 12) s.year + 1 to 1 else s.year to s.month + 1
            s.copy(year = y, month = m, selectedDay = null, selectedEvent = null)
        }
        loadCurrentMonth()
    }

    fun switchView(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        if (mode == ViewMode.LIST && !_uiState.value.yearEventsLoaded && !_uiState.value.isYearLoading) {
            loadYearEvents()
        }
    }

    private fun loadYearEvents() {
        val year = _uiState.value.todayYear
        val hadData = _uiState.value.yearEvents.isNotEmpty()
        _uiState.update { it.copy(isYearLoading = !hadData) }
        viewModelScope.launch {
            repository.getYearEvents(year).fold(
                onSuccess = { events ->
                    _uiState.update {
                        it.copy(yearEvents = events, yearEventsLoaded = true, isYearLoading = false)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isYearLoading = false, error = if (hadData) it.error else (err.message ?: "연간 일정 로딩 실패"))
                    }
                },
            )
        }
    }

    private fun loadCurrentMonth() {
        val (year, month) = _uiState.value.run { year to month }
        val hadData = _uiState.value.events.isNotEmpty()
        _uiState.update { it.copy(isLoading = !hadData, error = null) }
        viewModelScope.launch {
            repository.getEvents(year, month).fold(
                onSuccess = { events -> _uiState.update { it.copy(events = events, isLoading = false) } },
                onFailure = { err   -> _uiState.update { it.copy(isLoading = false, error = if (hadData) it.error else (err.message ?: "캘린더 로딩 실패")) } },
            )
        }
    }
}
