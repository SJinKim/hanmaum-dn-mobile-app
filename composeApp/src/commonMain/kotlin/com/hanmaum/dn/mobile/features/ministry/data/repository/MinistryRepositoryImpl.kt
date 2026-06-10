package com.hanmaum.dn.mobile.features.ministry.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.ContactResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistryDetailResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistrySummaryResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.ScheduleResponse
import com.hanmaum.dn.mobile.features.ministry.domain.model.Contact
import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.model.Schedule
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class MinistryRepositoryImpl(
    private val client: HttpClient,
) : MinistryRepository {

    override suspend fun getMinistries(activeOnly: Boolean): Result<List<Ministry>> = runCatching {
        val response = client.get("ministries?active=$activeOnly")
        val body = response.body<ApiResponse<List<MinistrySummaryResponse>>>()
        body.data?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail> = runCatching {
        val response = client.get("ministries/$publicId")
        val body = response.body<ApiResponse<MinistryDetailResponse>>()
        body.data?.toDomain() ?: error("Ministry detail data is null")
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private fun MinistrySummaryResponse.toDomain() = Ministry(
        publicId = publicId,
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        contacts = contacts.map { it.toDomain() },
        isActive = active,
    )

    private fun MinistryDetailResponse.toDomain() = MinistryDetail(
        publicId = publicId,
        title = title,
        subtitle = subtitle,
        about = about,
        requirements = requirements,
        schedules = schedules.map { it.toDomain() },
        contacts = contacts.map { it.toDomain() },
        imageUrl = imageUrl,
        isActive = active,
    )

    private fun ScheduleResponse.toDomain() = Schedule(
        description = description,
        startTime = startTime,
        endTime = endTime,
    )

    private fun ContactResponse.toDomain() = Contact(
        role = role,
        name = name,
    )
}
