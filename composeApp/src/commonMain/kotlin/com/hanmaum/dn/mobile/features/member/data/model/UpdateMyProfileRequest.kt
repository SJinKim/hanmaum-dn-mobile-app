package com.hanmaum.dn.mobile.features.member.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateMyProfileRequest(
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
)
