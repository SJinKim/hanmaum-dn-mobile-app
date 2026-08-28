package com.hanmaum.dn.mobile.features.attendance.domain.model

/**
 * Confirmation of an accepted attendance check-in.
 *
 * Mirrors the server's privacy-minimized response (PR#80): definition identity and
 * the calendar date only — no member identity, no exact timestamp.
 */
data class AttendanceCheckIn(
    val definitionPublicId: String,
    val definitionTitle: String,
    val attendanceDate: String, // ISO "yyyy-MM-dd"
)
