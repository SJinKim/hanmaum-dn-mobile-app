package com.hanmaum.dn.mobile.features.notification.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UnseenCountResponse(
    val count: Long = 0,
)
