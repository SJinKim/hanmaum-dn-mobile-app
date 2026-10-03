package com.hanmaum.dn.mobile.features.attendance.data.model

import kotlinx.serialization.Serializable

/**
 * Optional body of `POST /attendance/check-in` (#146).
 *
 * Raw device values only — the server decides whether the member was on site
 * and answers with `presence`. All three fields travel together: the server
 * rejects a body carrying only some of them with a 400.
 */
@Serializable
data class AttendanceCheckInRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
)
