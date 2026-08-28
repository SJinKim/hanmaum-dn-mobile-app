package com.hanmaum.dn.mobile.features.notification.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationPageResponse(
    val items: List<NotificationResponse> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = false,
)

@Serializable
data class NotificationResponse(
    val publicId: String,
    val type: String? = null,
    val title: String = "",
    val body: String = "",
    val referenceType: String? = null,
    val referencePublicId: String? = null,
    val createdAt: String? = null,
    val seenAt: String? = null,
    val readAt: String? = null,
)
