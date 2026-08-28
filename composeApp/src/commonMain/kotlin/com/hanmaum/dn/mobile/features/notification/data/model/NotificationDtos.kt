package com.hanmaum.dn.mobile.features.notification.data.model

import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationReference
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val publicId: String,
    val type: String,
    val title: String,
    val body: String,
    val referenceType: String? = null,
    val referencePublicId: String? = null,
    val createdAt: String,
    val seenAt: String? = null,
    val readAt: String? = null,
) {
    fun toDomain(): Notification {
        val refType = referenceType?.let { rt -> NotificationType.entries.find { it.name == rt } }
        return Notification(
            publicId = publicId,
            type = NotificationType.entries.find { it.name == type } ?: NotificationType.UNKNOWN,
            title = title,
            body = body,
            reference = if (refType != null && referencePublicId != null) {
                NotificationReference(refType, referencePublicId)
            } else {
                null
            },
            createdAt = Instant.parse(createdAt),
            isSeen = seenAt != null,
            isRead = readAt != null,
        )
    }
}

@Serializable
data class NotificationPageResponse(
    val items: List<NotificationResponse>,
    val page: Int,
    val hasNext: Boolean,
)

@Serializable
data class UnseenCountResponse(val count: Int)

@Serializable
data class RegisterDeviceTokenRequest(val token: String, val platform: String)

@Serializable
data class NotificationSettingsDto(val pushEnabled: Boolean)
