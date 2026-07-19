package com.hanmaum.dn.mobile.core.push

data class PushTapPayload(
    val type: String?,
    val referenceType: String?,
    val referencePublicId: String?,
    val notificationPublicId: String?,
)

/** Returns null when the map carries none of our data keys (e.g. a bare FCM system map). */
fun parsePushTap(data: Map<String, String>): PushTapPayload? {
    val payload = PushTapPayload(
        type = data["type"],
        referenceType = data["referenceType"],
        referencePublicId = data["referencePublicId"],
        notificationPublicId = data["notificationPublicId"],
    )
    return if (payload.type == null && payload.referenceType == null &&
        payload.referencePublicId == null && payload.notificationPublicId == null
    ) null else payload
}
