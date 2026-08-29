package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateRegistrationRequest(
    val period: String,
    val note: String? = null,
)
