package com.hanmaum.dn.mobile.features.ministry.domain.model

data class Ministry(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val contacts: List<Contact>,
    val isActive: Boolean,
)

data class MinistryDetail(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val about: String,
    val requirements: List<String>,
    val schedules: List<Schedule>,
    val contacts: List<Contact>,
    val imageUrl: String?,
    val isActive: Boolean,
)

data class Schedule(
    val description: String,
    val startTime: String,
    val endTime: String,
)

data class Contact(
    val role: String,
    val name: String,
)
