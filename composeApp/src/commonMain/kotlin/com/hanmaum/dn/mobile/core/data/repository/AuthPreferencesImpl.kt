package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.russhwolf.settings.Settings

class AuthPreferencesImpl(private val settings: Settings) : AuthPreferences {

    override fun isKeepSignedInEnabled(): Boolean = settings.getBoolean(KEY_KEEP, true)

    override fun setKeepSignedInEnabled(value: Boolean) {
        settings.putBoolean(KEY_KEEP, value)
        // Face ID unlocks a stored session; without one there is nothing to unlock.
        if (!value) settings.putBoolean(KEY_BIO, false)
    }

    override fun isBiometricEnabled(): Boolean = settings.getBoolean(KEY_BIO, false)

    override fun setBiometricEnabled(value: Boolean) = settings.putBoolean(KEY_BIO, value)

    private companion object {
        const val KEY_KEEP = "auth_keep_signed_in"
        const val KEY_BIO = "auth_biometric_enabled"
    }
}
