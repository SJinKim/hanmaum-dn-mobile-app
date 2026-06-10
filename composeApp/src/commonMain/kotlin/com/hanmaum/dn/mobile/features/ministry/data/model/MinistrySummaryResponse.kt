package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MinistrySummaryResponse(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val contacts: List<ContactResponse> = emptyList(),
    val active: Boolean,
)
