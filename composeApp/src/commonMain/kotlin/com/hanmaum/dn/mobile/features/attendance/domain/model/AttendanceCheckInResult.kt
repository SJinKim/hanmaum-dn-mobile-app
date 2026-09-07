package com.hanmaum.dn.mobile.features.attendance.domain.model

/** A check-in outcome without leaking Ktor/HTTP types into presentation code. */
sealed interface AttendanceCheckInResult {
    data class Success(val checkIn: AttendanceCheckIn) : AttendanceCheckInResult
    data object AlreadyCheckedIn : AttendanceCheckInResult
    data object OutsideWindow : AttendanceCheckInResult
    data object Failed : AttendanceCheckInResult
}
