package com.hanmaum.dn.mobile.features.notification.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.notification.data.model.NotificationPageResponse
import com.hanmaum.dn.mobile.features.notification.data.model.NotificationResponse
import com.hanmaum.dn.mobile.features.notification.data.model.UnseenCountResponse
import com.hanmaum.dn.mobile.features.notification.domain.model.AppNotification
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post

class NotificationRepositoryImpl(
    private val client: HttpClient,
) : NotificationRepository {

    override suspend fun getUnseenCount(): Result<Int> = runCatching {
        val body = client.get("me/notifications/unseen-count")
            .body<ApiResponse<UnseenCountResponse>>()
        (body.data?.count ?: 0L).toInt()
    }

    override suspend fun getNotifications(page: Int, size: Int): Result<List<AppNotification>> = runCatching {
        val body = client.get("me/notifications?page=$page&size=$size")
            .body<ApiResponse<NotificationPageResponse>>()
        body.data?.items.orEmpty().map { it.toDomain() }
    }

    override suspend fun markSeen(): Result<Unit> = runCatching {
        client.post("me/notifications/mark-seen")
        Unit
    }

    override suspend fun markAllRead(): Result<Unit> = runCatching {
        client.post("me/notifications/read-all")
        Unit
    }

    private fun NotificationResponse.toDomain() = AppNotification(
        publicId = publicId,
        title = title,
        body = body,
        referenceType = referenceType,
        referencePublicId = referencePublicId,
        createdAt = createdAt,
        isRead = readAt != null,
    )
}
