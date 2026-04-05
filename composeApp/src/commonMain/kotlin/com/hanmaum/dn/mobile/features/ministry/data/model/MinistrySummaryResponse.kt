package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MinistrySummaryResponse(
    val publicId: String,
    val name: String,
    val shortDescription: String,
    val imageUrl: String? = null,
    val leaderName: String? = null,
    val isActive: Boolean,
)
