// features/attendance/presentation/AttendanceUiState.kt
package com.hanmaum.dn.mobile.features.attendance.presentation

import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition

data class AttendanceUiState(
    val definition: AttendanceDefinition? = null, // null = no service scheduled today
    val isInWindow: Boolean = false,              // current time is within windowStart..windowEnd
    val isCheckedIn: Boolean = false,
    val isCheckingIn: Boolean = false,
    val checkInError: String? = null,
)
