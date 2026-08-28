package com.hanmaum.dn.mobile.features.notification.domain.model

/**
 * One entry in the notification centre.
 *
 * [referenceType] is what the row's icon and accent are chosen from — the
 * server's `type` vocabulary is open-ended, so anything unrecognised falls
 * back to the neutral treatment rather than rendering blank.
 */
data class AppNotification(
    val publicId: String,
    val title: String,
    val body: String,
    val referenceType: String?,
    val referencePublicId: String?,
    val createdAt: String?,
    val isRead: Boolean,
)
