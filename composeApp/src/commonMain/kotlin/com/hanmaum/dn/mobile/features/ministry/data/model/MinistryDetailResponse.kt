package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MinistryDetailResponse(
    val publicId: String,
    val title: String,
    val subtitle: String? = null,
    val about: String? = null,
    val requirements: List<String> = emptyList(),
    val schedules: List<ScheduleResponse> = emptyList(),
    val contacts: List<ContactResponse> = emptyList(),
    val imageUrl: String? = null,
    val active: Boolean = true,
)
