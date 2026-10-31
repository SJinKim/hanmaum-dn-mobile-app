package com.hanmaum.dn.mobile.features.consent

import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPolicy
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentStatus
import com.hanmaum.dn.mobile.features.consent.domain.model.MemberConsent
import com.hanmaum.dn.mobile.features.consent.domain.model.MemberConsents
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentRepository
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentSessionRepository
import kotlin.time.Instant

val TestPrivacyPolicy = ConsentPolicy(ConsentPurpose.APP_PRIVACY, "policy-1", true)
fun testConsent(state: ConsentStatus = ConsentStatus.GRANTED, version: String = "policy-1") = MemberConsent(
    ConsentPurpose.APP_PRIVACY, state, true, version,
    version.takeIf { state == ConsentStatus.GRANTED }, Instant.parse("2026-10-24T12:00:00Z").takeIf { state == ConsentStatus.GRANTED },
)

class FakeConsentRepository : ConsentRepository {
    var policyResult: Result<List<ConsentPolicy>> = Result.success(listOf(TestPrivacyPolicy))
    var summaryResult: Result<MemberConsents> = Result.success(MemberConsents(false, listOf(testConsent())))
    var grantResult: Result<MemberConsent> = Result.success(testConsent())
    var withdrawResult: Result<Unit> = Result.success(Unit)
    var grantCalls = 0
    var grantedVersion: String? = null
    var withdrawCalls = 0
    var policyCalls = 0
    var summaryCalls = 0
    var afterWithdraw: (() -> Unit)? = null
    var afterGrant: (() -> Unit)? = null
    var awaitSummary: (suspend () -> Unit)? = null
    override suspend fun policies(): Result<List<ConsentPolicy>> { policyCalls++; return policyResult }
    override suspend fun myConsents(): Result<MemberConsents> { summaryCalls++; awaitSummary?.invoke(); return summaryResult }
    override suspend fun grant(purpose: ConsentPurpose, version: String): Result<MemberConsent> {
        grantCalls++; grantedVersion = version; afterGrant?.invoke(); return grantResult
    }
    override suspend fun withdraw(purpose: ConsentPurpose): Result<Unit> { withdrawCalls++; afterWithdraw?.invoke(); return withdrawResult }
}

class FakeConsentSession : ConsentSessionRepository {
    var identity: String? = "member-1"
    var logoutCalls = 0
    var forgotBiometric = false
    var starts = 0
    var stops = 0
    override fun scope(): String? = identity
    override suspend fun logout(forgetBiometric: Boolean) {
        logoutCalls++; forgotBiometric = forgetBiometric; identity = null
    }
    override suspend fun startLocation() { starts++ }
    override suspend fun stopLocation() { stops++ }
}
