package com.hanmaum.dn.mobile.core.util

actual object AppConfig {
    // 10.0.2.2 ist die magische IP für "localhost" aus dem Android Emulator
    actual fun getBackendUrl(): String {
        return "http://10.0.2.2:8080/api/v1"
    }

    actual fun getKeycloakUrl(): String {
        return "http://10.0.2.2:8091"
    }
}