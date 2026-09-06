package com.hanmaum.dn.mobile.features.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent
import com.hanmaum.dn.mobile.features.calendar.domain.repository.CalendarRepository
import kotlinx.coroutines.Job
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

    private var monthJob: Job? = null
    private var monthJobKey: Pair<Int, Int>? = null
    private var yearJob: Job? = null

    init {
        // Entering the screen creates the view model, so the first load belongs
        // here rather than in a composable effect: it cannot be lost to a screen
        // rewrite the way `LaunchedEffect(Unit) { refresh() }` was in the v2 pass.
        loadCurrentMonth(force = true)
    }

    /**
     * Reloads what is on screen. Called on every resume, so it deliberately does
     * *not* re-issue a request that is already in flight for the same month —
     * that is what would turn the first resume after `init` into a double fetch.
     */
    fun refresh() {
        loadCurrentMonth(force = false)
        if (_uiState.value.viewMode == ViewMode.LIST) {
            loadYearEvents(force = false)
        } else {
            // The year list is off screen and now stale; drop the loaded marker so
            // switching to 목록 refetches instead of showing pre-refresh data.
            _uiState.update { it.copy(yearEventsLoaded = false) }
        }
    }

    fun retryMonth() = loadCurrentMonth(force = true)

    fun retryYear() = loadYearEvents(force = true)

    fun selectDay(day: Int) {
        _uiState.update { it.copy(selectedDay = if (it.selectedDay == day) null else day) }
    }

    fun selectEvent(event: CalendarEvent) {
        _uiState.update { it.copy(selectedEvent = event) }
    }

    fun dismissEventDetail() {
        _uiState.update { it.copy(selectedEvent = null) }
    }

    // Both month steps drop `events`: keeping them would leave the previous
    // month's list rendered under the new month's header until the fetch lands.
    fun previousMonth() {
        _uiState.update { s ->
            val (y, m) = if (s.month == 1) s.year - 1 to 12 else s.year to s.month - 1
            s.copy(year = y, month = m, selectedDay = null, selectedEvent = null, events = emptyList())
        }
        loadCurrentMonth(force = true)
    }

    fun nextMonth() {
        _uiState.update { s ->
            val (y, m) = if (s.month == 12) s.year + 1 to 1 else s.year to s.month + 1
            s.copy(year = y, month = m, selectedDay = null, selectedEvent = null, events = emptyList())
        }
        loadCurrentMonth(force = true)
    }

    fun switchView(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        if (mode == ViewMode.LIST && !_uiState.value.yearEventsLoaded) {
            loadYearEvents(force = false)
        }
    }

    private fun loadYearEvents(force: Boolean) {
        val year = _uiState.value.todayYear
        if (!force && yearJob?.isActive == true) return
        yearJob?.cancel()

        val hadData = _uiState.value.yearEvents.isNotEmpty()
        _uiState.update { it.copy(isYearLoading = !hadData, yearLoadFailed = false) }
        yearJob = viewModelScope.launch {
            repository.getYearEvents(year).fold(
                onSuccess = { events ->
                    _uiState.update {
                        it.copy(
                            yearEvents = events,
                            yearEventsLoaded = true,
                            isYearLoading = false,
                            yearLoadFailed = false,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isYearLoading = false, yearLoadFailed = true) }
                },
            )
        }
    }

    private fun loadCurrentMonth(force: Boolean) {
        val key = _uiState.value.run { year to month }
        if (!force && monthJob?.isActive == true && monthJobKey == key) return
        monthJob?.cancel()
        monthJobKey = key

        val (year, month) = key
        val hadData = _uiState.value.events.isNotEmpty()
        _uiState.update { it.copy(isLoading = !hadData, monthLoadFailed = false) }
        monthJob = viewModelScope.launch {
            val result = repository.getEvents(year, month)
            // Second line of defence behind the cancel above. Each stops a stale
            // month on its own (both were removed to check), but cancellation
            // only holds because the dispatcher refuses to resume a cancelled
            // job — the repository's runCatching would otherwise swallow the
            // CancellationException and report a failure against the new month.
            // This says the rule outright: only the month on screen may write.
            if (_uiState.value.run { this.year to this.month } != key) return@launch
            result.fold(
                onSuccess = { events ->
                    _uiState.update { it.copy(events = events, isLoading = false, monthLoadFailed = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, monthLoadFailed = true) }
                },
            )
        }
    }
}
