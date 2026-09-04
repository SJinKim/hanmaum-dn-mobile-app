package com.hanmaum.dn.mobile.features.attendance

import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckIn
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceHistory
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceSummary
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository

class FakeAttendanceRepository : AttendanceRepository {
    var definitionsResult: Result<List<AttendanceDefinition>> = Result.success(emptyList())
    var checkInResult: Result<AttendanceCheckIn> = Result.success(
        AttendanceCheckIn(definitionPublicId = "def-1", definitionTitle = "Sunday Service", attendanceDate = "2026-06-15"),
    )

    var summaryResult: Result<AttendanceSummary> = Result.success(
        AttendanceSummary(monthAttended = 0, monthTotal = 0, yearAttended = 0, yearToDateTotal = 0, rate = 0.0),
    )
    var historyResult: Result<AttendanceHistory> = Result.success(
        AttendanceHistory(from = "2026-06-06", to = "2026-09-04", entries = emptyList()),
    )

    override suspend fun getActiveDefinitions(): Result<List<AttendanceDefinition>> = definitionsResult
    override suspend fun checkIn(): Result<AttendanceCheckIn> = checkInResult
    override suspend fun getMySummary(): Result<AttendanceSummary> = summaryResult
    override suspend fun getMyHistory(): Result<AttendanceHistory> = historyResult
}
