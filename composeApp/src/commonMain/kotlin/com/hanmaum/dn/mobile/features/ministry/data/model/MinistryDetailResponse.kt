package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MinistryDetailResponse(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val about: String,
    val requirements: List<String> = emptyList(),
    val schedules: List<ScheduleResponse> = emptyList(),
    val contacts: List<ContactResponse> = emptyList(),
    val imageUrl: String? = null,
    val active: Boolean,
)
