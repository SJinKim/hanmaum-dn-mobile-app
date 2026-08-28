package com.hanmaum.dn.mobile.features.events.domain.model

data class EventRsvpCheckIn(
    val eventPublicId: String,
    val eventTitle: String,
    val checkedInAt: String,
)
