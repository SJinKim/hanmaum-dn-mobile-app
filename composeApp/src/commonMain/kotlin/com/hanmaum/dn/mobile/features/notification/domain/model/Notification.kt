package com.hanmaum.dn.mobile.features.notification.domain.model

import kotlinx.datetime.Instant

enum class NotificationType { ANNOUNCEMENT, EVENT_REMINDER, UNKNOWN }

data class NotificationReference(
    val type: NotificationType,
    val publicId: String,
)

data class Notification(
    val publicId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val reference: NotificationReference?,
    val createdAt: Instant,
    val isSeen: Boolean,
    val isRead: Boolean,
)

data class NotificationPage(
    val items: List<Notification>,
    val hasNext: Boolean,
)
