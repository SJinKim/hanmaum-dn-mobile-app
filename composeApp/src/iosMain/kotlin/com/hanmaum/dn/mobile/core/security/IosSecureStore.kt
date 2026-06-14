package com.hanmaum.dn.mobile.core.security

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings

/**
 * Backs [SecureStore] with the iOS Keychain via multiplatform-settings'
 * [KeychainSettings]. Items are stored under a dedicated service and persist in
 * the Keychain (encrypted by the OS), separate from NSUserDefaults.
 */
@OptIn(ExperimentalSettingsImplementation::class)
class IosSecureStore : SecureStore {

    private val settings = KeychainSettings(service = SERVICE)

    override fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    override fun getString(key: String): String? = settings.getStringOrNull(key)

    override fun remove(key: String) {
        settings.remove(key)
    }

    private companion object {
        const val SERVICE = "com.hanmaum.dn.mobile.credentials"
    }
}
