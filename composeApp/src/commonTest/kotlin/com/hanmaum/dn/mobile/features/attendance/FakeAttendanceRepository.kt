package com.hanmaum.dn.mobile.features.attendance

import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckIn
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository

class FakeAttendanceRepository : AttendanceRepository {
    var definitionsResult: Result<List<AttendanceDefinition>> = Result.success(emptyList())
    var checkInResult: Result<AttendanceCheckIn> = Result.success(
        AttendanceCheckIn(definitionPublicId = "def-1", definitionTitle = "Sunday Service", attendanceDate = "2026-06-15"),
    )

    override suspend fun getActiveDefinitions(): Result<List<AttendanceDefinition>> = definitionsResult
    override suspend fun checkIn(): Result<AttendanceCheckIn> = checkInResult
}
