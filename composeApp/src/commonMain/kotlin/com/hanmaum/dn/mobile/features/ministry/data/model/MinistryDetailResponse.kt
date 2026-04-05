package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MinistryDetailResponse(
    val publicId: String,
    val name: String,
    val shortDescription: String,
    val longDescription: String? = null,
    val imageUrl: String? = null,
    val leader: LeaderResponse? = null,
    val isActive: Boolean,
)

@Serializable
data class LeaderResponse(
    val publicId: String,
    val fullName: String,
)
