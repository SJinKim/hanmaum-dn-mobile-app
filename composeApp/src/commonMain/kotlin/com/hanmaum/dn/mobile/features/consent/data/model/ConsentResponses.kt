package com.hanmaum.dn.mobile.features.consent.data.model

import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentGrant
import kotlinx.serialization.Serializable

/*
 * Wire shapes of hanmaum-dn-server's ConsentDtos (hanmaum-dn-server#358).
 *
 * Enums stay strings here and are mapped in the repository, so a purpose added on the server
 * drops one row instead of failing the whole list. The booleans the server always sends have
 * no default, so a renamed key fails the call instead of arriving as a plausible false.
 */

@Serializable
data class ConsentPolicyResponse(
    val purpose: String,
    val version: String,
    val requiredForAccount: Boolean,
)

@Serializable
data class MemberConsentStatusResponse(
    val purpose: String,
    val state: String,
    val requiredForAccount: Boolean,
    val currentVersion: String,
    val grantedVersion: String? = null,
    val grantedAt: String? = null,
)

@Serializable
data class MemberConsentsResponse(
    val actionRequired: Boolean,
    val consents: List<MemberConsentStatusResponse>,
)

@Serializable
data class GrantConsentRequest(
    val purpose: String,
    val consent: ConsentGrant,
)
