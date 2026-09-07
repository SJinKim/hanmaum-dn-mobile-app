package com.hanmaum.dn.mobile.features.attendance

import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckIn
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckInResult
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceHistory
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceSummary
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository

class FakeAttendanceRepository : AttendanceRepository {
    var definitionsResult: Result<List<AttendanceDefinition>> = Result.success(emptyList())
    var checkInResult: AttendanceCheckInResult = AttendanceCheckInResult.Success(
        AttendanceCheckIn(definitionPublicId = "def-1", definitionTitle = "Sunday Service", attendanceDate = "2026-06-15"),
    )
    var checkInCallCount = 0
    var onCheckIn: (() -> Unit)? = null

    var summaryResult: Result<AttendanceSummary> = Result.success(
        AttendanceSummary(monthAttended = 0, monthTotal = 0, yearAttended = 0, yearToDateTotal = 0, rate = 0.0),
    )
    var historyResult: Result<AttendanceHistory> = Result.success(
        AttendanceHistory(from = "2026-06-06", to = "2026-09-04", entries = emptyList()),
    )
    var summaryCallCount = 0
    var historyCallCount = 0

    override suspend fun getActiveDefinitions(): Result<List<AttendanceDefinition>> = definitionsResult
    override suspend fun checkIn(): AttendanceCheckInResult {
        checkInCallCount++
        onCheckIn?.invoke()
        return checkInResult
    }
    override suspend fun getMySummary(): Result<AttendanceSummary> {
        summaryCallCount++
        return summaryResult
    }
    var lastHistoryFrom: String? = null
    var lastHistoryTo: String? = null
    override suspend fun getMyHistory(from: String?, to: String?): Result<AttendanceHistory> {
        historyCallCount++
        lastHistoryFrom = from; lastHistoryTo = to
        return historyResult
    }
}
