package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

/**
 * MinistryScheduleDto. Times are ISO local times ("07:00", or "07:00:00");
 * [dayOfWeek] is a java.time.DayOfWeek name ("MONDAY" … "SUNDAY").
 */
@Serializable
data class ScheduleResponse(
    val description: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val location: String? = null,
    val dayOfWeek: String? = null,
)
