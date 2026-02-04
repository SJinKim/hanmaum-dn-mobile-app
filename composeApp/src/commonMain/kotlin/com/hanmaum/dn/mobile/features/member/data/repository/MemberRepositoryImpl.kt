package com.hanmaum.dn.mobile.features.member.data.repository

import com.hanmaum.dn.mobile.core.domain.model.Member
import com.hanmaum.dn.mobile.core.util.AppConfig
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

    override suspend fun getMyProfile(): Result<Member> {
        return try {
            val url = "${AppConfig.getBackendUrl()}/members/me"

            val response = client.get(url) {
                contentType(ContentType.Application.Json)
                // WICHTIG: Der Token wird normalerweise über dein AuthPlugin im HttpClient
                // automatisch gesetzt, wenn er in den Preferences gespeichert ist.
                // Falls du KEIN AuthPlugin nutzt, musst du hier den Header manuell setzen:
                // header("Authorization", "Bearer $gespeicherterToken")
            }

            if (response.status == HttpStatusCode.OK) {
                val member = response.body<Member>()
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