package com.hanmaum.dn.mobile.features.member.data.repository

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
            val myProfileUrl = "members/me"

            val response = client.get(myProfileUrl) {
                contentType(ContentType.Application.Json)
            }

            if (response.status == HttpStatusCode.OK) {
                val member = response.body<MemberResponse>()
                Result.success(member)
            } else {
                Result.failure(Exception($$"Profil konnte nicht geladen werden (${response.status})"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}