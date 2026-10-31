package com.hanmaum.dn.mobile.features.consent.domain.model

import kotlin.time.Instant

enum class ConsentPurpose { APP_PRIVACY, COURSE_APPLICATION }
enum class ConsentStatus { GRANTED, OUTDATED, MISSING }

data class ConsentPolicy(val purpose: ConsentPurpose, val version: String, val requiredForAccount: Boolean)

data class MemberConsent(
    val purpose: ConsentPurpose,
    val state: ConsentStatus,
    val requiredForAccount: Boolean,
    val currentVersion: String,
    val grantedVersion: String?,
    val grantedAt: Instant?,
) {
    val isCurrent: Boolean get() = state == ConsentStatus.GRANTED && grantedVersion == currentVersion
}

data class MemberConsents(val actionRequired: Boolean, val consents: List<MemberConsent>) {
    val privacy: MemberConsent? get() = consents.singleOrNull { it.purpose == ConsentPurpose.APP_PRIVACY }
    val allowsAccess: Boolean get() = !actionRequired &&
        privacy?.isCurrent == true && consents.none { it.requiredForAccount && !it.isCurrent }
}

enum class ConsentProblem { LOAD, SAVE, VERSION_OUTDATED, REQUIRED, POLICY_UNAVAILABLE, WITHDRAW }
class ConsentException(val problem: ConsentProblem) : Exception(problem.name)

/** An unset or non-HTTPS policy must never become a fictitious link. */
data class PrivacyPolicyLink(val url: String?) {
    val isAvailable: Boolean get() = url != null
}

data class PolicyConsentState(
    val isLoading: Boolean = true,
    val policy: ConsentPolicy? = null,
    val acceptedVersion: String? = null,
    val problem: ConsentProblem? = null,
) {
    val accepted: Boolean get() = policy != null && acceptedVersion == policy.version
    val canSubmit: Boolean get() = !isLoading && policy != null && accepted &&
        problem != ConsentProblem.POLICY_UNAVAILABLE
}
