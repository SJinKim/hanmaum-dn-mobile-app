package com.hanmaum.dn.mobile.features.attendance

import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository

class FakeAttendanceRepository : AttendanceRepository {
    var definitionsResult: Result<List<AttendanceDefinition>> = Result.success(emptyList())
    var checkInResult: Result<Unit> = Result.success(Unit)

    override suspend fun getActiveDefinitions(): Result<List<AttendanceDefinition>> = definitionsResult
    override suspend fun checkIn(): Result<Unit> = checkInResult
}
