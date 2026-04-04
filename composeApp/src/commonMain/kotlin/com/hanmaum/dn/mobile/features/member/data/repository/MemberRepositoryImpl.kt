package com.hanmaum.dn.mobile.features.member.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class MemberRepositoryImpl(
    private val client: HttpClient
) : MemberRepository {

    override suspend fun getMyProfile(): Result<MemberResponse> {
        return try {
            val response = client.get("members/me") {
                contentType(ContentType.Application.Json)
            }
            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<ApiResponse<MemberResponse>>()
                val member = apiResponse.data
                    ?: return Result.failure(Exception("Profile data is null"))
                Result.success(member)
            } else {
                Result.failure(Exception("Profile load failed (${response.status})"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
