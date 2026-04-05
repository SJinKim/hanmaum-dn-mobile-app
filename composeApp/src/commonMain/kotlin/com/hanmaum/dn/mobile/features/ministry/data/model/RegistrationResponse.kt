package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResponse(
    val publicId: String,
    val ministryPublicId: String,
    val memberPublicId: String,
    val memberName: String,
    val registrationPeriod: String,
    val note: String? = null,
    val status: String,
)
