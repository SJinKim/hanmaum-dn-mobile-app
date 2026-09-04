package com.hanmaum.dn.mobile.features.attendance.domain.repository

import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckIn
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceHistory
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceSummary

interface AttendanceRepository {
    /** Returns only active definitions. */
    suspend fun getActiveDefinitions(): Result<List<AttendanceDefinition>>
    /** Posts check-in for the authenticated user. Server validates time window. */
    suspend fun checkIn(): Result<AttendanceCheckIn>

    /** The caller's own counters for the month and the year so far. */
    suspend fun getMySummary(): Result<AttendanceSummary>

    /**
     * The caller's own occurrences, newest first.
     *
     * With no range the server uses its own: `to` is today, `from` 90 days
     * earlier. Pass ISO dates to widen it — the server caps `to` at today and
     * rejects spans over 366 days with a 400, so a calendar asks for a year and
     * filters the months itself rather than requesting one month at a time.
     */
    suspend fun getMyHistory(from: String? = null, to: String? = null): Result<AttendanceHistory>
}
