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

data class MyRegistration(
    val publicId: String,
    val status: RegistrationStatus,
    val note: String?,
)

enum class RegistrationStatus {
    PENDING,
    APPROVED,
    NONE, // no record exists, or backend returned REJECTED (treat as re-apply)
}