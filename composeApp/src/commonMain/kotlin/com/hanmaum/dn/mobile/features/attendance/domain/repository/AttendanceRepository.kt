package com.hanmaum.dn.mobile.features.attendance.domain.repository

import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition

interface AttendanceRepository {
    /** Returns only active definitions. */
    suspend fun getActiveDefinitions(): Result<List<AttendanceDefinition>>
    /** Posts check-in for the authenticated user. Server validates time window. */
    suspend fun checkIn(): Result<Unit>
}
