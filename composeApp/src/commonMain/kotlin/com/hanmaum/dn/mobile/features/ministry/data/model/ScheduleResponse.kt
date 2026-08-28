package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val description: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)
