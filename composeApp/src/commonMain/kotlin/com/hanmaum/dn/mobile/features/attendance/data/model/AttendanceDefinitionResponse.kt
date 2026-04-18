package com.hanmaum.dn.mobile.features.attendance.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceDefinitionResponse(
    val publicId: String,
    val title: String,
    val dayOfWeek: String,   // "MONDAY" … "SUNDAY" (Java DayOfWeek.name())
    val windowStart: String, // "HH:mm:ss"
    val windowEnd: String,   // "HH:mm:ss"
    val isActive: Boolean,
)
