// features/attendance/presentation/AttendanceViewModel.kt
package com.hanmaum.dn.mobile.features.attendance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.AttendancePreferences
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init { load() }

    fun load() {
        viewModelScope.launch {
            repository.getActiveDefinitions().fold(
                onSuccess = { definitions ->
                    val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val today = now.date.toString() // ISO "yyyy-MM-dd"
                    val todayName = now.dayOfWeek.name // "MONDAY" … "SUNDAY"
                    val todayDef = definitions.firstOrNull { it.dayOfWeek == todayName }
                    // Restore the checked-in state from a prior session: PR#80 removed the
                    // server-side "/logs/me", so the local record is the only way to know.
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
    private fun loadStats() {
        viewModelScope.launch {
            repository.getMySummary().fold(
                onSuccess = { summary -> _uiState.update { it.copy(summary = summary) } },
                onFailure = { err -> println("[AttendanceViewModel] Failed to load summary: ${err.message}") },
            )
        }
        viewModelScope.launch {
            repository.getMyHistory().fold(
                onSuccess = { history ->
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
            repository.checkIn().fold(
                onSuccess = { checkIn ->
                    preferences.markCheckedIn(checkIn.definitionPublicId, checkIn.attendanceDate)
                    _uiState.update { it.copy(isCheckedIn = true, isCheckingIn = false, checkedInDate = checkIn.attendanceDate) }
                },
                onFailure = { err ->
                    val status = (err as? ClientRequestException)?.response?.status
                    when (status) {
                        // 409: the server already recorded today's check-in. Persist it locally
                        // so the checked-in state is restored on the next launch.
                        HttpStatusCode.Conflict    -> {
                            markCheckedInForToday()
                            _uiState.update { it.copy(isCheckedIn = true, isCheckingIn = false, checkedInDate = todayIso()) }
                        }
                        HttpStatusCode.BadRequest  -> _uiState.update { it.copy(isCheckingIn = false, checkInError = "출석 시간이 아닙니다") }
                        else                       -> _uiState.update { it.copy(isCheckingIn = false, checkInError = "출석 처리에 실패했습니다") }
                    }
                },
            )
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
