package com.hanmaum.dn.mobile.features.notification.domain.repository

import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage

interface NotificationRepository {
    suspend fun getNotifications(page: Int): Result<NotificationPage>
    suspend fun getUnseenCount(): Result<Int>
    suspend fun markAllSeen(): Result<Unit>
    suspend fun markRead(publicId: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
    suspend fun getPushEnabled(): Result<Boolean>
    suspend fun setPushEnabled(enabled: Boolean): Result<Unit>
    suspend fun registerDeviceToken(token: String, platform: String): Result<Unit>
    suspend fun deleteDeviceToken(token: String): Result<Unit>
}
