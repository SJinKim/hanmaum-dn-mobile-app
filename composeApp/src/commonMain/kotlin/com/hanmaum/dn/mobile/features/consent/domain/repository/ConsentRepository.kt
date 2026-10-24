package com.hanmaum.dn.mobile.features.consent.domain.repository

import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentOverview
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPolicy
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentResult
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentStatus

/** hanmaum-dn-server's consent endpoints (hanmaum-dn-server#358), shared by every screen that asks. */
interface ConsentRepository {

    /** The versions in force. Public, so registration can ask before an account exists. */
    suspend fun getPolicies(): ConsentResult<List<ConsentPolicy>>

    /** The signed-in member's standing per purpose. Answers for PENDING members too. */
    suspend fun getMyConsents(): ConsentResult<ConsentOverview>

    /** Records agreement to [version] of [purpose]'s text, the version the screen showed. */
    suspend fun grant(purpose: ConsentPurpose, version: String): ConsentResult<ConsentStatus>

    /** Withdraws [purpose]. Idempotent: withdrawing twice is a success. */
    suspend fun withdraw(purpose: ConsentPurpose): ConsentResult<Unit>
}

/**
 * True only when the server says so. A failed check lets the member through: the server
 * enforces the lock itself, and a network hiccup must not lock a member out of the app.
 */
suspend fun ConsentRepository.requiresConsent(): Boolean =
    (getMyConsents() as? ConsentResult.Success)?.data?.actionRequired == true
