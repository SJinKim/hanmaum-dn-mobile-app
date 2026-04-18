package com.hanmaum.dn.mobile.features.attendance.domain.model

data class AttendanceDefinition(
    val publicId: String,
    val title: String,
    val dayOfWeek: String,   // "MONDAY" … "SUNDAY"
    val windowStart: String, // "HH:mm:ss"
    val windowEnd: String,   // "HH:mm:ss"
)
