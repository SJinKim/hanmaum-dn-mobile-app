package com.hanmaum.dn.mobile.features.ministry.domain.model

import kotlinx.datetime.DayOfWeek

data class Ministry(
    val publicId: String,
    val name: String,
    val shortDescription: String,
    val imageUrl: String?,
    val leaderName: String?,
    val isActive: Boolean,
    val memberCount: Int = 0,
)

data class MinistryDetail(
    val publicId: String,
    val name: String,
    val shortDescription: String,
    val longDescription: String?,
    val imageUrl: String?,
    val leaderName: String?,
    val isActive: Boolean,
    val schedules: List<MinistrySchedule> = emptyList(),
)

data class MinistrySchedule(
    val description: String?,
    val dayOfWeek: DayOfWeek?,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
)
