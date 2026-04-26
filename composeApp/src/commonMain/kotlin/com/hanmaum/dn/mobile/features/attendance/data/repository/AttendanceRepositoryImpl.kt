package com.hanmaum.dn.mobile.features.attendance.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.attendance.data.model.AttendanceDefinitionResponse
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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

    override suspend fun checkIn(): Result<Unit> = runCatching {
        client.post("attendance/check-in")
        Unit
    }

    private fun AttendanceDefinitionResponse.toDomain() = AttendanceDefinition(
        publicId    = publicId,
        title       = title,
        dayOfWeek   = dayOfWeek,
        windowStart = windowStart,
        windowEnd   = windowEnd,
    )
}
