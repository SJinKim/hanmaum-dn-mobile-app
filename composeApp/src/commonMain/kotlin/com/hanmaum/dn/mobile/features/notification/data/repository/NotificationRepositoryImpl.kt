package com.hanmaum.dn.mobile.features.notification.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.notification.data.model.NotificationPageResponse
import com.hanmaum.dn.mobile.features.notification.data.model.NotificationSettingsDto
import com.hanmaum.dn.mobile.features.notification.data.model.RegisterDeviceTokenRequest
import com.hanmaum.dn.mobile.features.notification.data.model.UnseenCountResponse
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class NotificationRepositoryImpl(
    private val client: HttpClient,
) : NotificationRepository {

    override suspend fun getNotifications(page: Int): Result<NotificationPage> = runCatching {
        val response = client.get("me/notifications?page=$page&size=20")
        check(response.status.isSuccess()) { "알림을 불러오지 못했습니다 (${response.status})" }
        val body = response.body<ApiResponse<NotificationPageResponse>>()
        val data = body.data ?: error("알림 응답이 비어 있습니다")
        NotificationPage(items = data.items.map { it.toDomain() }, hasNext = data.hasNext)
    }

    override suspend fun getUnseenCount(): Result<Int> = runCatching {
        val response = client.get("me/notifications/unseen-count")
        check(response.status.isSuccess()) { "unseen count failed (${response.status})" }
        response.body<ApiResponse<UnseenCountResponse>>().data?.count ?: 0
    }

    override suspend fun markAllSeen(): Result<Unit> = simplePost("me/notifications/mark-seen")

    override suspend fun markRead(publicId: String): Result<Unit> = simplePost("me/notifications/$publicId/read")

    override suspend fun markAllRead(): Result<Unit> = simplePost("me/notifications/read-all")

    override suspend fun delete(publicId: String): Result<Unit> = runCatching {
        val response = client.delete("me/notifications/$publicId")
        check(response.status.isSuccess()) { "notification delete failed (${response.status})" }
    }

    override suspend fun deleteAll(): Result<Unit> = runCatching {
        val response = client.delete("me/notifications")
        check(response.status.isSuccess()) { "notifications clear failed (${response.status})" }
    }

    override suspend fun getPushEnabled(): Result<Boolean> = runCatching {
        val response = client.get("me/notification-settings")
        check(response.status.isSuccess()) { "settings load failed (${response.status})" }
        response.body<ApiResponse<NotificationSettingsDto>>().data?.pushEnabled ?: true
    }

    override suspend fun setPushEnabled(enabled: Boolean): Result<Unit> = runCatching {
        val response = client.put("me/notification-settings") {
            contentType(ContentType.Application.Json)
            setBody(NotificationSettingsDto(pushEnabled = enabled))
        }
        check(response.status.isSuccess()) { "settings save failed (${response.status})" }
    }

    override suspend fun registerDeviceToken(token: String, platform: String): Result<Unit> = runCatching {
        val response = client.put("me/device-tokens") {
            contentType(ContentType.Application.Json)
            setBody(RegisterDeviceTokenRequest(token = token, platform = platform))
        }
        check(response.status.isSuccess()) { "token register failed (${response.status})" }
    }

    override suspend fun deleteDeviceToken(token: String): Result<Unit> = runCatching {
        val response = client.delete("me/device-tokens/$token")
        check(response.status.isSuccess()) { "token delete failed (${response.status})" }
    }

    private suspend fun simplePost(path: String): Result<Unit> = runCatching {
        val response = client.post(path)
        check(response.status.isSuccess()) { "$path failed (${response.status})" }
    }
}
