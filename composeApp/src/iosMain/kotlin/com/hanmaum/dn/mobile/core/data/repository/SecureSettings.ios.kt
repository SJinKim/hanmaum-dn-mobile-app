package com.hanmaum.dn.mobile.core.data.repository

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

@OptIn(ExperimentalSettingsImplementation::class)
actual class SecureSettingsFactory {
    actual fun create(): Settings = KeychainSettings(service = SERVICE)

    private companion object {
        const val SERVICE = "com.hanmaum.dn.mobile.tokens"
    }
}
