package com.hanmaum.dn.mobile.features.ministry.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistryDetailResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistrySummaryResponse
import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
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
        name = name,
        shortDescription = shortDescription,
        imageUrl = imageUrl,
        leaderName = contacts.firstOrNull()?.name,
        isActive = isActive,
    )

    private fun MinistryDetailResponse.toDomain() = MinistryDetail(
        publicId = publicId,
        name = name,
        shortDescription = shortDescription,
        longDescription = longDescription,
        imageUrl = imageUrl,
        leaderName = contacts.firstOrNull()?.name,
        isActive = isActive,
    )
}
