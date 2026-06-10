package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactResponse(
    val role: String,
    val name: String,
)
