package com.hanmaum.dn.mobile.core.util

expect object AppConfig {
    fun getBackendUrl(): String
    fun getKeycloakUrl(): String
}