package com.hanmaum.dn.mobile.features.consent

import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentOverview
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPolicy
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentResult
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentState
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentStatus
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentRepository
import kotlin.time.Instant

/** Hand-written, like every other fake here — there is no mocking library. */
class FakeConsentRepository : ConsentRepository {

    var policiesResult: ConsentResult<List<ConsentPolicy>> = ConsentResult.Success(listOf(policy()))
    var myConsentsResult: ConsentResult<ConsentOverview> = ConsentResult.Success(overview(ConsentState.GRANTED))
    var grantResult: ConsentResult<ConsentStatus> = ConsentResult.Success(status(ConsentState.GRANTED))
    var withdrawResult: ConsentResult<Unit> = ConsentResult.Success(Unit)

    var policiesCalls = 0
    var myConsentsCalls = 0
    var grantCalls = 0
    var lastGrant: Pair<ConsentPurpose, String>? = null
    var withdrawCalls = 0
    var lastWithdrawn: ConsentPurpose? = null

    override suspend fun getPolicies(): ConsentResult<List<ConsentPolicy>> {
        policiesCalls++
        return policiesResult
    }

    override suspend fun getMyConsents(): ConsentResult<ConsentOverview> {
        myConsentsCalls++
        return myConsentsResult
    }

    override suspend fun grant(purpose: ConsentPurpose, version: String): ConsentResult<ConsentStatus> {
        grantCalls++
        lastGrant = purpose to version
        return grantResult
    }

    override suspend fun withdraw(purpose: ConsentPurpose): ConsentResult<Unit> {
        withdrawCalls++
        lastWithdrawn = purpose
        return withdrawResult
    }

    companion object {
        fun policy(version: String = "2026-09") =
            ConsentPolicy(ConsentPurpose.APP_PRIVACY, version, requiredForAccount = true)

        fun status(
            state: ConsentState,
            currentVersion: String = "2026-09",
            grantedVersion: String? = if (state == ConsentState.MISSING) null else currentVersion,
        ) = ConsentStatus(
            purpose = ConsentPurpose.APP_PRIVACY,
            state = state,
            requiredForAccount = true,
            currentVersion = currentVersion,
            grantedVersion = grantedVersion,
            grantedAt = if (grantedVersion == null) null else Instant.parse("2026-09-01T10:00:00Z"),
        )

        fun overview(state: ConsentState, grantedVersion: String? = null) = ConsentOverview(
            actionRequired = state != ConsentState.GRANTED,
            consents = listOf(
                if (grantedVersion == null) status(state) else status(state, grantedVersion = grantedVersion),
            ),
        )
    }
}
