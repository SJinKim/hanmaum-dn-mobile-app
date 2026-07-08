package com.hanmaum.dn.mobile.features.member.data.model

import kotlinx.serialization.Serializable

/**
 * PATCH /members/me body. Wire names are camelCase — the backend is default
 * Jackson (no snake_case strategy); a snake_case key is silently ignored.
 * Null fields are omitted (encodeDefaults=false) and mean "keep old value".
 */
@Serializable
data class UpdateMyProfileRequest(
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val zipCode: String? = null,
    val city: String? = null,
)
