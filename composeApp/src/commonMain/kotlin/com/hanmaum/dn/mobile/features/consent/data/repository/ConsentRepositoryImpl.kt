package com.hanmaum.dn.mobile.features.consent.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.core.domain.model.ApiErrorResponse
import com.hanmaum.dn.mobile.features.consent.data.model.ConsentGrantDto
import com.hanmaum.dn.mobile.features.consent.data.model.ConsentPolicyDto
import com.hanmaum.dn.mobile.features.consent.data.model.GrantConsentRequest
import com.hanmaum.dn.mobile.features.consent.data.model.MemberConsentDto
import com.hanmaum.dn.mobile.features.consent.data.model.MemberConsentsDto
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentException
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPolicy
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentProblem
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentStatus
import com.hanmaum.dn.mobile.features.consent.domain.model.MemberConsent
import com.hanmaum.dn.mobile.features.consent.domain.model.MemberConsents
import com.hanmaum.dn.mobile.features.consent.domain.model.PrivacyPolicyLink
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlin.time.Instant

class ConsentRepositoryImpl(private val client: HttpClient) : ConsentRepository {
    override suspend fun policies(): Result<List<ConsentPolicy>> = request {
        // Purposes this app version does not know are skipped: a newer server must not break loading.
        read<List<ConsentPolicyDto>>(client.get("consents/policies")).mapNotNull {
            val purpose = knownPurpose(it.purpose) ?: return@mapNotNull null
            ConsentPolicy(purpose, validVersion(it.version), it.requiredForAccount)
        }.also { policies ->
            require(policies.map { it.purpose }.distinct().size == policies.size)
            require(policies.any { it.purpose == ConsentPurpose.APP_PRIVACY })
        }
    }

    override suspend fun myConsents(): Result<MemberConsents> = request {
        val dto = read<MemberConsentsDto>(client.get("members/me/consents"))
        MemberConsents(dto.actionRequired, dto.consents.mapNotNull { it.toDomain() }).also {
            require(it.privacy != null)
            require(it.consents.map { status -> status.purpose }.distinct().size == it.consents.size)
        }
    }

    override suspend fun grant(purpose: ConsentPurpose, version: String): Result<MemberConsent> = request {
        read<MemberConsentDto>(client.post("members/me/consents") {
            contentType(ContentType.Application.Json)
            setBody(GrantConsentRequest(purpose.name, ConsentGrantDto(true, validVersion(version))))
        }).toDomain().also {
            require(it != null && it.purpose == purpose && it.isCurrent && it.grantedVersion == version)
        }!!
    }

    override suspend fun withdraw(purpose: ConsentPurpose): Result<Unit> = request {
        val response = client.delete("members/me/consents/${purpose.name}")
        if (response.status != HttpStatusCode.NoContent) throw failure(response)
    }

    private suspend inline fun <reified T> read(response: HttpResponse): T {
        if (!response.status.isSuccess()) throw failure(response)
        val body = response.body<ApiResponse<T>>()
        if (!body.success) throw ConsentException(ConsentProblem.LOAD)
        return body.data ?: throw ConsentException(ConsentProblem.LOAD)
    }

    private suspend fun failure(response: HttpResponse): ConsentException {
        val code = try {
            response.body<ApiErrorResponse>().code
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return ConsentException(when (code) {
            "CONSENT_VERSION_OUTDATED" -> ConsentProblem.VERSION_OUTDATED
            "CONSENT_REQUIRED" -> ConsentProblem.REQUIRED
            else -> ConsentProblem.LOAD
        })
    }

    private suspend fun <T> request(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun validVersion(value: String): String = value.also { require(it.isNotBlank() && it.length <= 50) }

private fun knownPurpose(value: String): ConsentPurpose? = ConsentPurpose.entries.firstOrNull { it.name == value }

private fun MemberConsentDto.toDomain(): MemberConsent? {
    val purpose = knownPurpose(purpose) ?: return null
    val status = ConsentStatus.entries.firstOrNull { it.name == state } ?: return null
    return MemberConsent(purpose, status, requiredForAccount, validVersion(currentVersion), grantedVersion,
        grantedAt?.let { runCatching { Instant.parse(it) }.getOrNull() })
}

fun privacyPolicyLink(value: String): PrivacyPolicyLink = PrivacyPolicyLink(
    runCatching { Url(value) }.getOrNull()?.takeIf {
        it.protocol == URLProtocol.HTTPS && it.host.isNotBlank() && it.user.isNullOrBlank() && it.password.isNullOrBlank()
    }?.toString(),
)
