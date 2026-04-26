// features/attendance/presentation/AttendanceViewModel.kt
package com.hanmaum.dn.mobile.features.attendance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AttendanceViewModel(
    private val repository: AttendanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            repository.getActiveDefinitions().fold(
                onSuccess = { definitions ->
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val todayName = now.dayOfWeek.name // "MONDAY" … "SUNDAY"
                    val todayDef = definitions.firstOrNull { it.dayOfWeek == todayName }
                    _uiState.update { it.copy(
                        definition  = todayDef,
                        isInWindow  = todayDef?.let { d -> isCurrentlyInWindow(d, now.hour, now.minute, now.second) } ?: false,
                    )}
                },
                onFailure = { err -> println("[AttendanceViewModel] Failed to load definitions: ${err.message}") },
            )
        }
    }

    fun checkIn() {
        if (_uiState.value.isCheckedIn || _uiState.value.isCheckingIn) return
        _uiState.update { it.copy(isCheckingIn = true, checkInError = null) }
        viewModelScope.launch {
            repository.checkIn().fold(
                onSuccess = {
                    _uiState.update { it.copy(isCheckedIn = true, isCheckingIn = false) }
                },
                onFailure = { err ->
                    val status = (err as? ClientRequestException)?.response?.status
                    when (status) {
                        HttpStatusCode.Conflict    -> _uiState.update { it.copy(isCheckedIn = true, isCheckingIn = false) }
                        HttpStatusCode.BadRequest  -> _uiState.update { it.copy(isCheckingIn = false, checkInError = "출석 시간이 아닙니다") }
                        else                       -> _uiState.update { it.copy(isCheckingIn = false, checkInError = "출석 처리에 실패했습니다") }
                    }
                },
            )
        }
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
