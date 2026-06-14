package com.hanmaum.dn.mobile.core.data.repository

import com.russhwolf.settings.Settings

/**
 * A [Settings] instance backed by the platform's hardware-backed secure store —
 * iOS Keychain, Android EncryptedSharedPreferences. Used only for secrets
 * (auth tokens); non-secret prefs (locale, theme) keep the plain `Settings()`.
 *
 * On Android the implementation needs a [android.content.Context], so the actual
 * is created inside the Koin platform module (which has `androidContext()`),
 * not as a no-arg factory.
 */
expect class SecureSettingsFactory {
    fun create(): Settings
}
