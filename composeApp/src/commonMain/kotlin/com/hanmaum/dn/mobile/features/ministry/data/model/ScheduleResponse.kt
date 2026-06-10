package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val description: String,
    val startTime: String,
    val endTime: String,
)
