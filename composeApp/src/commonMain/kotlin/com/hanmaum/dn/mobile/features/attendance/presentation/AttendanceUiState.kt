// features/attendance/presentation/AttendanceUiState.kt
package com.hanmaum.dn.mobile.features.attendance.presentation

import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceEntry
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceSummary

data class AttendanceUiState(
    val definition: AttendanceDefinition? = null, // null = no service scheduled today
    val isInWindow: Boolean = false,              // current time is within windowStart..windowEnd
    val isCheckedIn: Boolean = false,
    val isCheckingIn: Boolean = false,
    val checkInError: String? = null,
    val checkedInDate: String? = null,            // ISO date of the recorded check-in (server-confirmed)

    /** Counters for the three tiles; null while unloaded or after a failure. */
    val summary: AttendanceSummary? = null,
    /** 최근 출석, newest first. */
    val history: List<AttendanceEntry> = emptyList(),
    /**
     * Distinguishes "loaded and there is nothing" from "not loaded yet".
     * Without it an empty list and a failed request look identical, and the
     * screen would claim a member never attended when the call simply failed.
     */
    val historyLoaded: Boolean = false,
)
