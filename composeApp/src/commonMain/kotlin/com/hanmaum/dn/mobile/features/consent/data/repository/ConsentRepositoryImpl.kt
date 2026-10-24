package com.hanmaum.dn.mobile.features.consent.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiErrorResponse
import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.consent.data.model.ConsentPolicyResponse
import com.hanmaum.dn.mobile.features.consent.data.model.GrantConsentRequest
import com.hanmaum.dn.mobile.features.consent.data.model.MemberConsentStatusResponse
import com.hanmaum.dn.mobile.features.consent.data.model.MemberConsentsResponse
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentGrant
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentOverview
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPolicy
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentResult
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentState
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentStatus
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

class ConsentRepositoryImpl(
    private val client: HttpClient,
) : ConsentRepository {

    override suspend fun getPolicies(): ConsentResult<List<ConsentPolicy>> = call(
        request = { client.get("consents/policies") },
        read = { response ->
            response.body<ApiResponse<List<ConsentPolicyResponse>>>().data?.mapNotNull { it.toDomain() }
        },
    )

    override suspend fun getMyConsents(): ConsentResult<ConsentOverview> = call(
        request = { client.get("members/me/consents") },
        read = { response -> response.body<ApiResponse<MemberConsentsResponse>>().data?.toDomain() },
    )

    override suspend fun grant(purpose: ConsentPurpose, version: String): ConsentResult<ConsentStatus> = call(
        request = {
            client.post("members/me/consents") {
                contentType(ContentType.Application.Json)
                setBody(GrantConsentRequest(purpose.name, ConsentGrant(accepted = true, version = version)))
            }
        },
        read = { response -> response.body<ApiResponse<MemberConsentStatusResponse>>().data?.toDomain() },
    )

    // 204 has no body to read, so success is the status alone.
    override suspend fun withdraw(purpose: ConsentPurpose): ConsentResult<Unit> = call(
        request = { client.delete("members/me/consents/${purpose.name}") },
        read = { },
    )

    /** The client has expectSuccess = false, so non-2xx comes back normally and is mapped here. */
    private suspend fun <T> call(
        request: suspend () -> HttpResponse,
        read: suspend (HttpResponse) -> T?,
    ): ConsentResult<T> = try {
        val response = request()
        if (response.status.isSuccess()) {
            read(response)?.let { ConsentResult.Success(it) } ?: ConsentResult.Failed
        } else {
            failure(response.errorBody())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ConsentResult.Failed
    }

    private suspend fun HttpResponse.errorBody(): ApiErrorResponse? =
        runCatching { body<ApiErrorResponse>() }.getOrNull()

    /** One outcome per code (hanmaum-dn-server's ApiErrorCode); the HTTP status alone decides nothing. */
    private fun failure(error: ApiErrorResponse?): ConsentResult<Nothing> = when (error?.code) {
        CODE_VERSION_OUTDATED -> ConsentResult.VersionOutdated
        CODE_CONSENT_REQUIRED -> ConsentResult.ConsentRequired
        else -> ConsentResult.Failed
    }

    private fun ConsentPolicyResponse.toDomain(): ConsentPolicy? {
        val purpose = purpose.toPurposeOrNull() ?: return null
        return ConsentPolicy(purpose = purpose, version = version, requiredForAccount = requiredForAccount)
    }

    // A purpose this build does not know is dropped; an unknown state counts as MISSING, so the
    // app asks rather than assuming agreement.
    private fun MemberConsentsResponse.toDomain() = ConsentOverview(
        actionRequired = actionRequired,
        consents = consents.mapNotNull { it.toDomain() },
    )

    private fun MemberConsentStatusResponse.toDomain(): ConsentStatus? {
        val purpose = purpose.toPurposeOrNull() ?: return null
        return ConsentStatus(
            purpose = purpose,
            state = ConsentState.entries.firstOrNull { it.name == state } ?: ConsentState.MISSING,
            requiredForAccount = requiredForAccount,
            currentVersion = currentVersion,
            grantedVersion = grantedVersion,
            grantedAt = grantedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        )
    }

    private fun String.toPurposeOrNull(): ConsentPurpose? = ConsentPurpose.entries.firstOrNull { it.name == this }

    internal companion object {
        // Shared with registration and the global consent lock, which read the same codes.
        const val CODE_VERSION_OUTDATED = "CONSENT_VERSION_OUTDATED"
        const val CODE_CONSENT_REQUIRED = "CONSENT_REQUIRED"
    }
}
