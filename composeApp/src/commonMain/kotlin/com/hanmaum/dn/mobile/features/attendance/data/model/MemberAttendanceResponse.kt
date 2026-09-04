package com.hanmaum.dn.mobile.features.attendance.data.model

import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /api/v1/me/attendance/summary`, copied field for field from
 * `MemberAttendanceSummaryResponse` in the server. Jackson runs with no naming
 * strategy there, so these names are camelCase verbatim.
 *
 * No defaults: the server always sends all five, and a default would turn a
 * future rename into a silent zero instead of a failing test — the mistake
 * `MinistrySummaryResponse` made (#129).
 */
@Serializable
data class AttendanceSummaryResponse(
    val monthAttended: Int,
    val monthTotal: Int,
    val yearAttended: Int,
    val yearToDateTotal: Int,
    val rate: Double,
)

/** One occurrence in `GET /api/v1/me/attendance`. */
@Serializable
data class AttendanceEntryResponse(
    val definitionPublicId: String,
    val definitionTitle: String,
    val date: String,          // ISO "2026-09-04"
    val checkedIn: Boolean,
    val checkedInAt: String? = null, // ISO instant; null for a missed occurrence
)

/**
 * `from` and `to` echo the range the server actually used — both query
 * parameters are optional and `to` is capped at today — so the UI can label the
 * list with the range it really got rather than the one it asked for.
 */
@Serializable
data class AttendanceHistoryResponse(
    val from: String,
    val to: String,
    val entries: List<AttendanceEntryResponse> = emptyList(),
)
