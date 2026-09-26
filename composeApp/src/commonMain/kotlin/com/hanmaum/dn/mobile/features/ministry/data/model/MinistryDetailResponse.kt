package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire names differ from the domain names — see [MinistrySummaryResponse]. */
@Serializable
data class MinistryDetailResponse(
    val publicId: String,
    @SerialName("title") val name: String,
    @SerialName("subtitle") val shortDescription: String,
    @SerialName("about") val longDescription: String? = null,
    val imageUrl: String? = null,
    val contacts: List<MinistryContactResponse> = emptyList(),
    val requirements: List<String> = emptyList(),
    val schedules: List<ScheduleResponse> = emptyList(),
    val leaderName: String? = null,
    val isActive: Boolean,
)
