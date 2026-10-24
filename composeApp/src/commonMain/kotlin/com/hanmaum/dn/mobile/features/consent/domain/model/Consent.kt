package com.hanmaum.dn.mobile.features.consent.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/** What a member agrees to; the names are hanmaum-dn-server's ConsentPurpose. */
enum class ConsentPurpose {
    /** Processing for church activity. Required: without it no account exists or works. */
    APP_PRIVACY,

    /** Handing the 양육 application to application.hanmaum.de. */
    COURSE_APPLICATION,
}

enum class ConsentState {
    /** A standing grant of the version in force. */
    GRANTED,

    /** A standing grant of an older version; the member is asked again. */
    OUTDATED,

    /** Never granted, or withdrawn. */
    MISSING,
}

/** The policy text in force for one purpose, as GET /consents/policies lists it. */
data class ConsentPolicy(
    val purpose: ConsentPurpose,
    val version: String,
    val requiredForAccount: Boolean,
)

data class ConsentStatus(
    val purpose: ConsentPurpose,
    val state: ConsentState,
    val requiredForAccount: Boolean,
    val currentVersion: String,
    val grantedVersion: String?,
    val grantedAt: Instant?,
)

data class ConsentOverview(
    /** True if a purpose required for the account is not GRANTED; the app asks before anything else. */
    val actionRequired: Boolean,
    val consents: List<ConsentStatus>,
) {
    fun of(purpose: ConsentPurpose): ConsentStatus? = consents.firstOrNull { it.purpose == purpose }
}

/**
 * A member's agreement, embedded in every request that needs one (hanmaum-dn-server's ConsentGrant).
 *
 * [version] is the version the screen showed, never a constant: the server answers a stale one with
 * 409 CONSENT_VERSION_OUTDATED instead of recording agreement to a text the member never saw.
 */
@Serializable
data class ConsentGrant(
    val accepted: Boolean,
    val version: String,
)
