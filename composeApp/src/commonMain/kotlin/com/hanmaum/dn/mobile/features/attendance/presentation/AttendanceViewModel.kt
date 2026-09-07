// features/attendance/presentation/AttendanceViewModel.kt
package com.hanmaum.dn.mobile.features.attendance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.AttendancePreferences
import com.hanmaum.dn.mobile.core.domain.repository.RecordedAttendanceCheckIn
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckInResult
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val preferences: AttendancePreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()
    private var isFirstResume = true

    init {
        observeSharedCheckInStatus()
        load()
    }

    /**
     * Refresh after returning from another destination or from the background.
     * The first resume is already covered by init and is skipped to avoid
     * sending every initial request twice.
     */
    fun onResume() {
        if (isFirstResume) {
            isFirstResume = false
        } else {
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            repository.getActiveDefinitions().fold(
                onSuccess = { definitions ->
                    val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val today = now.date.toString() // ISO "yyyy-MM-dd"
                    val todayName = now.dayOfWeek.name // "MONDAY" … "SUNDAY"
                    val todayDef = definitions.firstOrNull { it.dayOfWeek == todayName }
                    // Restore immediately from the persisted shared status. The
                    // concurrent history refresh reconciles it with the server.
                    val alreadyCheckedIn = todayDef != null && preferences.isCheckedIn(todayDef.publicId, today)
                    _uiState.update { it.copy(
                        definition    = todayDef,
                        isInWindow    = todayDef?.let { d -> isCurrentlyInWindow(d, now.hour, now.minute, now.second) } ?: false,
                        isCheckedIn   = alreadyCheckedIn,
                        checkedInDate = if (alreadyCheckedIn) today else null,
                    )}
                },
                onFailure = { err -> println("[AttendanceViewModel] Failed to load definitions: ${err.message}") },
            )
        }
        loadStats()
    }

    /**
     * The counters and the recent list. Deliberately its own coroutine rather
     * than chained onto the definitions call: neither needs the other, and a
     * failing summary must not cost the user the check-in slider.
     */
    private fun loadStats(reconcileCheckInStatus: Boolean = true) {
        viewModelScope.launch {
            repository.getMySummary().fold(
                onSuccess = { summary -> _uiState.update { it.copy(summary = summary) } },
                onFailure = { err -> println("[AttendanceViewModel] Failed to load summary: ${err.message}") },
            )
        }
        viewModelScope.launch {
            val statusAtRequestStart = preferences.lastCheckIn.value
            repository.getMyHistory().fold(
                onSuccess = { history ->
                    if (reconcileCheckInStatus) {
                        val serverCheckIn = history.entries
                            .firstOrNull { it.checkedIn && it.date == todayIso() }
                        if (serverCheckIn != null) {
                            preferences.markCheckedIn(serverCheckIn.definitionPublicId, serverCheckIn.date)
                        } else {
                            clearStaleLocalStatus(statusAtRequestStart)
                        }
                    }
                    _uiState.update { it.copy(history = history.entries, historyLoaded = true) }
                },
                onFailure = { err -> println("[AttendanceViewModel] Failed to load history: ${err.message}") },
            )
        }
    }

    fun checkIn() {
        if (_uiState.value.isCheckedIn || _uiState.value.isCheckingIn) return
        _uiState.update { it.copy(isCheckingIn = true, checkInError = null) }
        viewModelScope.launch {
            when (val result = repository.checkIn()) {
                is AttendanceCheckInResult.Success -> {
                    val checkIn = result.checkIn
                    preferences.markCheckedIn(checkIn.definitionPublicId, checkIn.attendanceDate)
                    _uiState.update { it.copy(isCheckedIn = true, isCheckingIn = false, checkedInDate = checkIn.attendanceDate) }
                }
                AttendanceCheckInResult.AlreadyCheckedIn -> {
                    markCheckedInForToday()
                    _uiState.update { it.copy(isCheckedIn = true, isCheckingIn = false, checkedInDate = todayIso()) }
                }
                AttendanceCheckInResult.OutsideWindow ->
                    _uiState.update { it.copy(isCheckingIn = false, checkInError = "출석 시간이 아닙니다") }
                AttendanceCheckInResult.Failed ->
                    _uiState.update { it.copy(isCheckingIn = false, checkInError = "출석 처리에 실패했습니다") }
            }
        }
    }

    private fun observeSharedCheckInStatus() {
        viewModelScope.launch {
            // load() applies the initial persisted value. Later emissions are
            // successful check-ins from another screen or server reconciliation.
            preferences.lastCheckIn.drop(1).collect { record ->
                val definition = _uiState.value.definition
                val isCheckedIn = record != null &&
                    definition != null &&
                    record.definitionId == definition.publicId &&
                    record.date == todayIso()
                _uiState.update {
                    it.copy(
                        isCheckedIn = isCheckedIn,
                        isCheckingIn = if (isCheckedIn) false else it.isCheckingIn,
                        checkedInDate = record?.date?.takeIf { isCheckedIn },
                    )
                }
                if (isCheckedIn) {
                    // The event itself is already server-confirmed. Refresh the
                    // visible metrics, but never let an older in-flight history
                    // snapshot undo the just-completed check-in.
                    loadStats(reconcileCheckInStatus = false)
                }
            }
        }
    }

    private fun clearStaleLocalStatus(statusAtRequestStart: RecordedAttendanceCheckIn?) {
        val current = preferences.lastCheckIn.value
        if (statusAtRequestStart != null && current == statusAtRequestStart && current.date == todayIso()) {
            preferences.clearCheckedIn(current.definitionId, current.date)
        }
    }

    private fun todayIso(): String =
        kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    private fun markCheckedInForToday() {
        val def = _uiState.value.definition ?: return
        preferences.markCheckedIn(def.publicId, todayIso())
    }

    /** Returns true if current time (hour:minute:second) falls within the definition's window. */
    private fun isCurrentlyInWindow(def: AttendanceDefinition, hour: Int, minute: Int, second: Int): Boolean {
        return try {
            val startParts = def.windowStart.split(":")
            val endParts   = def.windowEnd.split(":")
            val nowSecs    = hour * 3600 + minute * 60 + second
            val startSecs  = startParts[0].toInt() * 3600 + startParts[1].toInt() * 60 + startParts[2].toInt()
            val endSecs    = endParts[0].toInt()   * 3600 + endParts[1].toInt()   * 60 + endParts[2].toInt()
            nowSecs in startSecs..endSecs
        } catch (_: Exception) { false }
    }
}
