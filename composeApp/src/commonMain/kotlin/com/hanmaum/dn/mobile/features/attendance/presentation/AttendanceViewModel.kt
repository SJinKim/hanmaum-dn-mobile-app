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
                        isInWindow  = todayDef?.let { d -> isCurrentlyInWindow(d, now.hour, now.minute) } ?: false,
                    )}
                },
                onFailure = { /* silently hide card on error */ },
            )
        }
    }

    fun checkIn() {
        _uiState.update { it.copy(isCheckingIn = true, checkInError = null) }
        viewModelScope.launch {
            repository.checkIn().fold(
                onSuccess = {
                    _uiState.update { it.copy(isCheckedIn = true, isCheckingIn = false) }
                },
                onFailure = { err ->
                    val alreadyCheckedIn = (err as? ClientRequestException)
                        ?.response?.status == HttpStatusCode.Conflict // 409
                    if (alreadyCheckedIn) {
                        _uiState.update { it.copy(isCheckedIn = true, isCheckingIn = false) }
                    } else {
                        val msg = when ((err as? ClientRequestException)?.response?.status) {
                            HttpStatusCode.BadRequest -> "출석 시간이 아닙니다"
                            else                     -> "출석 처리에 실패했습니다"
                        }
                        _uiState.update { it.copy(isCheckingIn = false, checkInError = msg) }
                    }
                },
            )
        }
    }

    /** Returns true if current hour:minute falls within the definition's window. */
    private fun isCurrentlyInWindow(def: AttendanceDefinition, hour: Int, minute: Int): Boolean {
        return try {
            val startParts = def.windowStart.split(":")
            val endParts   = def.windowEnd.split(":")
            val now        = hour * 60 + minute
            val start      = startParts[0].toInt() * 60 + startParts[1].toInt()
            val end        = endParts[0].toInt()   * 60 + endParts[1].toInt()
            now in start..end
        } catch (_: Exception) { false }
    }
}
