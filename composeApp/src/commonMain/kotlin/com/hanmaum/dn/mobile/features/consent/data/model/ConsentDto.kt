package com.hanmaum.dn.mobile.features.consent.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ConsentPolicyDto(val purpose: String, val version: String, val requiredForAccount: Boolean)
@Serializable
internal data class MemberConsentDto(
    val purpose: String,
    val state: String,
    val requiredForAccount: Boolean,
    val currentVersion: String,
    val grantedVersion: String? = null,
    val grantedAt: String? = null,
)
@Serializable
internal data class MemberConsentsDto(val actionRequired: Boolean, val consents: List<MemberConsentDto>)
@Serializable
internal data class GrantConsentRequest(val purpose: String, val consent: ConsentGrantDto)
@Serializable
internal data class ConsentGrantDto(val accepted: Boolean, val version: String)
