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
     * The caller's own recent occurrences, newest first. The server resolves the
     * range itself when none is given: `to` is today, `from` 90 days earlier.
     */
    suspend fun getMyHistory(): Result<AttendanceHistory>
}
