package com.hanmaum.dn.mobile.core.push

interface PushManager {
    /** Wire value for registerDeviceToken: "ANDROID" or "IOS". */
    val platform: String
    suspend fun currentToken(): String?
    fun isPermissionGranted(): Boolean
    suspend fun requestPermission(): Boolean
}
