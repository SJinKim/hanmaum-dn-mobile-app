package com.hanmaum.dn.mobile.features.attendance.data.model

import kotlinx.serialization.Serializable

/**
 * Wire shape of `POST /attendance/check-in` (PR#80).
 *
 * Intentionally carries no member identity or exact timestamp — definition and
 * calendar date only.
 */
@Serializable
data class AttendanceCheckInResponse(
    val definitionPublicId: String,
    val definitionTitle: String,
    val attendanceDate: String, // "yyyy-MM-dd"
)
