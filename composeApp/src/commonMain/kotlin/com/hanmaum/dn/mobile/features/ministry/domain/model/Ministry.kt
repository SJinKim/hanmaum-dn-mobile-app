package com.hanmaum.dn.mobile.features.ministry.domain.model

data class Ministry(
    val publicId: String,
    val name: String,
    val shortDescription: String,
    val imageUrl: String?,
    val leaderName: String?,
    val isActive: Boolean,
)

data class MinistryDetail(
    val publicId: String,
    val name: String,
    val shortDescription: String,
    val longDescription: String?,
    val imageUrl: String?,
    val leaderName: String?,
    val isActive: Boolean,
)
