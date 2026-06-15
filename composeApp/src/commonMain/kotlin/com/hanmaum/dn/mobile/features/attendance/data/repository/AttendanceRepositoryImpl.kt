package com.hanmaum.dn.mobile.features.attendance.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.attendance.data.model.AttendanceCheckInResponse
import com.hanmaum.dn.mobile.features.attendance.data.model.AttendanceDefinitionResponse
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckIn
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
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
