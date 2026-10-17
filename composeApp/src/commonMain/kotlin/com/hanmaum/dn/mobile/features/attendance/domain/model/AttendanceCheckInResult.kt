package com.hanmaum.dn.mobile.features.attendance.domain.model

/** A check-in outcome without leaking Ktor/HTTP types into presentation code. */
sealed interface AttendanceCheckInResult {
    data class Success(val checkIn: AttendanceCheckIn) : AttendanceCheckInResult
    data object AlreadyCheckedIn : AttendanceCheckInResult
    data object OutsideWindow : AttendanceCheckInResult

    /** 403: the account may not check in, e.g. it is not ACTIVE (yet). */
    data object NotAllowed : AttendanceCheckInResult
    data object Failed : AttendanceCheckInResult
}
