package com.hanmaum.dn.mobile.core.security

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings

/**
 * Backs [SecureStore] with the iOS Keychain via multiplatform-settings'
 * [KeychainSettings]. Items are stored under a dedicated service and persist in
 * the Keychain (encrypted by the OS), separate from NSUserDefaults.
 *
 * Every access is guarded. The Keychain can refuse a request for reasons the
 * app cannot fix at runtime — a missing entitlement, a locked device, a
 * simulator quirk — and this store only ever backs the optional biometric
 * sign-in. Losing it must degrade to "no saved credentials", never take the
 * app down on launch.
 */
@OptIn(ExperimentalSettingsImplementation::class)
class IosSecureStore : SecureStore {

    private val settings: KeychainSettings? = runCatching { KeychainSettings(service = SERVICE) }.getOrNull()

    override fun putString(key: String, value: String) {
        runCatching { settings?.putString(key, value) }
    }

    override fun getString(key: String): String? =
        runCatching { settings?.getStringOrNull(key) }.getOrNull()

    override fun remove(key: String) {
        runCatching { settings?.remove(key) }
    }

    private companion object {
        const val SERVICE = "com.hanmaum.dn.mobile.credentials"
    }
}
