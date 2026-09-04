package com.hanmaum.dn.mobile.features.attendance.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.attendance.data.model.AttendanceCheckInResponse
import com.hanmaum.dn.mobile.features.attendance.data.model.AttendanceDefinitionResponse
import com.hanmaum.dn.mobile.features.attendance.data.model.AttendanceEntryResponse
import com.hanmaum.dn.mobile.features.attendance.data.model.AttendanceHistoryResponse
import com.hanmaum.dn.mobile.features.attendance.data.model.AttendanceSummaryResponse
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckIn
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceEntry
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceHistory
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceSummary
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post

class AttendanceRepositoryImpl(
    private val client: HttpClient,
) : AttendanceRepository {

    override suspend fun getActiveDefinitions(): Result<List<AttendanceDefinition>> = runCatching {
        val response = client.get("attendance/definitions?active=true")
        val body = response.body<ApiResponse<List<AttendanceDefinitionResponse>>>()
        body.data?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun checkIn(): Result<AttendanceCheckIn> = runCatching {
        // expectSuccess so 4xx throws ClientRequestException: the ViewModel relies on
        // 409 (already checked in) and 400 (outside window) to drive its UI states.
        val response = client.post("attendance/check-in") { expectSuccess = true }
        val body = response.body<ApiResponse<AttendanceCheckInResponse>>()
        body.data?.toDomain()
            ?: error("check-in response missing data")
    }

    override suspend fun getMySummary(): Result<AttendanceSummary> = runCatching {
        val response = client.get("me/attendance/summary")
        val body = response.body<ApiResponse<AttendanceSummaryResponse>>()
        body.data?.toDomain() ?: error("attendance summary response missing data")
    }

    override suspend fun getMyHistory(from: String?, to: String?): Result<AttendanceHistory> = runCatching {
        // Without a range the server defaults to the last 90 days up to today
        // and echoes back what it used.
        val query = listOfNotNull(
            from?.let { "from=$it" },
            to?.let { "to=$it" },
        ).joinToString("&")
        val response = client.get(if (query.isEmpty()) "me/attendance" else "me/attendance?$query")
        val body = response.body<ApiResponse<AttendanceHistoryResponse>>()
        body.data?.toDomain() ?: error("attendance history response missing data")
    }

    private fun AttendanceSummaryResponse.toDomain() = AttendanceSummary(
        monthAttended   = monthAttended,
        monthTotal      = monthTotal,
        yearAttended    = yearAttended,
        yearToDateTotal = yearToDateTotal,
        rate            = rate,
    )

    private fun AttendanceHistoryResponse.toDomain() = AttendanceHistory(
        from    = from,
        to      = to,
        // The server documents newest-first; sorting here as well keeps the UI
        // correct if that ever changes, and costs nothing at this size.
        entries = entries.sortedByDescending { it.date }.map { it.toDomain() },
    )

    private fun AttendanceEntryResponse.toDomain() = AttendanceEntry(
        definitionPublicId = definitionPublicId,
        definitionTitle    = definitionTitle,
        date               = date,
        checkedIn          = checkedIn,
        checkedInAt        = checkedInAt,
    )

    private fun AttendanceCheckInResponse.toDomain() = AttendanceCheckIn(
        definitionPublicId = definitionPublicId,
        definitionTitle    = definitionTitle,
        attendanceDate     = attendanceDate,
    )

    private fun AttendanceDefinitionResponse.toDomain() = AttendanceDefinition(
        publicId    = publicId,
        title       = title,
        dayOfWeek   = dayOfWeek,
        windowStart = windowStart,
        windowEnd   = windowEnd,
    )
}
