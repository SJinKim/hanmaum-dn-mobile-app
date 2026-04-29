package com.hanmaum.dn.mobile.features.member.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateMyProfileRequest(
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val street: String? = null,
    @kotlinx.serialization.SerialName("zip_code") val zipCode: String? = null,
    val city: String? = null,
)
