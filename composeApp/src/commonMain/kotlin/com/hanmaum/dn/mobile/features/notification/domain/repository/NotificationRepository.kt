package com.hanmaum.dn.mobile.features.notification.domain.repository

import com.hanmaum.dn.mobile.features.notification.domain.model.AppNotification

interface NotificationRepository {
    /** Number of notifications the member has not seen yet. */
    suspend fun getUnseenCount(): Result<Int>

    suspend fun getNotifications(page: Int = 0, size: Int = 30): Result<List<AppNotification>>

    /** Clears the bell badge — the member has now looked at the list. */
    suspend fun markSeen(): Result<Unit>

    suspend fun markAllRead(): Result<Unit>
}
