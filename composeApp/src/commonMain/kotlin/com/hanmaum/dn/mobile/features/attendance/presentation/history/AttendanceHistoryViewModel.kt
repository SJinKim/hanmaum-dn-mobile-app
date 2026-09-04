package com.hanmaum.dn.mobile.features.attendance.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceEntry
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Distinguishes an empty record from one that never arrived: an error must
 * never render as "you have never attended".
 */
data class AttendanceHistoryUiState(
    val isLoading: Boolean = true,
    val failed: Boolean = false,
    val year: Int = 0,
    val month: Int = 0,
    val selectedDay: Int? = null,
    /** Every attended occurrence in the loaded range, newest first. */
    val attended: List<AttendanceEntry> = emptyList(),
) {
    /** Days of the shown month that carry a mark. */
    val markedDays: Set<Int>
        get() {
            val prefix = "$year-${month.toString().padStart(2, '0')}-"
            return attended.mapNotNullTo(mutableSetOf()) { entry ->
                entry.date.takeIf { it.startsWith(prefix) }
                    ?.substring(prefix.length)?.toIntOrNull()
            }
        }

    /** Entries on the selected day, chronological — several 예배 can share a day. */
    val entriesForSelectedDay: List<AttendanceEntry>
        get() {
            val day = selectedDay ?: return emptyList()
            val key = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            return attended.filter { it.date == key }.sortedBy { it.checkedInAt ?: it.date }
        }
}

class AttendanceHistoryViewModel(
    private val repository: AttendanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val today = kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        _uiState.update { it.copy(year = today.year, month = today.month.ordinal + 1, selectedDay = today.day) }
        load()
    }

    /**
     * One request for the last year rather than one per month.
     *
     * The server caps a range at 366 days and rejects more with a 400, so a
     * year is both the most it will give in one call and enough for a calendar
     * a member scrolls back through. Months are filtered client-side, which
     * makes navigation instant and costs nothing extra.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true, failed = false) }
        viewModelScope.launch {
            val today = kotlin.time.Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val from = LocalDate(today.year - 1, today.month, 1).toString()
            repository.getMyHistory(from = from, to = today.toString()).fold(
                onSuccess = { history ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            failed = false,
                            // Only attended occurrences: this screen answers
                            // "when was I there", not "what did I miss".
                            attended = history.entries.filter { e -> e.checkedIn },
                        )
                    }
                },
                onFailure = { _uiState.update { it.copy(isLoading = false, failed = true) } },
            )
        }
    }

    fun selectDay(day: Int) = _uiState.update { it.copy(selectedDay = day) }

    fun previousMonth() = _uiState.update {
        val m = it.month - 1
        // Changing month clears the selection: keeping day 31 while moving to
        // February would select a day that does not exist.
        if (m < 1) it.copy(year = it.year - 1, month = 12, selectedDay = null)
        else it.copy(month = m, selectedDay = null)
    }

    fun nextMonth() = _uiState.update {
        val m = it.month + 1
        if (m > 12) it.copy(year = it.year + 1, month = 1, selectedDay = null)
        else it.copy(month = m, selectedDay = null)
    }
}
