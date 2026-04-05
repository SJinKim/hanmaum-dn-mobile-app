package com.hanmaum.dn.mobile.features.ministry.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.CreateRegistrationRequest
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistryDetailResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistrySummaryResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.RegistrationResponse
import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.model.MyRegistration
import com.hanmaum.dn.mobile.features.ministry.domain.model.RegistrationStatus
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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

    override suspend fun getMyRegistration(ministryPublicId: String): Result<MyRegistration?> = runCatching {
        val response = client.get("ministries/$ministryPublicId/registrations/me")
        if (response.status == HttpStatusCode.NotFound) return@runCatching null
        val body = response.body<ApiResponse<RegistrationResponse>>()
        body.data?.toDomain()
    }

    override suspend fun register(ministryPublicId: String, note: String?): Result<MyRegistration> = runCatching {
        val period = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year.toString()
        val response = client.post("ministries/$ministryPublicId/registrations") {
            contentType(ContentType.Application.Json)
            setBody(CreateRegistrationRequest(period = period, note = note))
        }
        val body = response.body<ApiResponse<RegistrationResponse>>()
        body.data?.toDomain() ?: error("Registration data is null")
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private fun MinistrySummaryResponse.toDomain() = Ministry(
        publicId = publicId,
        name = name,
        shortDescription = shortDescription,
        imageUrl = imageUrl,
        leaderName = leaderName,
        isActive = isActive,
    )

    private fun MinistryDetailResponse.toDomain() = MinistryDetail(
        publicId = publicId,
        name = name,
        shortDescription = shortDescription,
        longDescription = longDescription,
        imageUrl = imageUrl,
        leaderName = leader?.fullName,
        isActive = isActive,
    )

    private fun RegistrationResponse.toDomain() = MyRegistration(
        publicId = publicId,
        status = when (status) {
            "APPROVED" -> RegistrationStatus.APPROVED
            "PENDING" -> RegistrationStatus.PENDING
            else -> RegistrationStatus.NONE
        },
        note = note,
    )
}
