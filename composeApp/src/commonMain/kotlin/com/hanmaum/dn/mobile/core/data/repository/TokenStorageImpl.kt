package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.RefreshTokenVault
import com.russhwolf.settings.Settings

class TokenStorageImpl(
    private val settings: Settings,
    private val refreshVault: RefreshTokenVault,
) : TokenStorage {

    override fun saveAccessToken(token: String) {
        settings.putString(KEY_ACCESS, token)
    }

    override fun getAccessToken(): String? = settings.getStringOrNull(KEY_ACCESS)

    override fun saveRefreshToken(token: String?) {
        if (token != null) refreshVault.store(token) else refreshVault.clear()
    }

    // In-memory only: returns the token unlocked for this session, else null.
    override fun getRefreshToken(): String? = refreshVault.current()

    override fun clear() {
        settings.remove(KEY_ACCESS)
        refreshVault.clear()
        settings.remove(KEY_KEEP_SIGNED_IN)
        settings.remove(KEY_BIOMETRIC)
    }

    override fun setKeepSignedIn(value: Boolean) {
        settings.putBoolean(KEY_KEEP_SIGNED_IN, value)
    }

    override fun isKeepSignedIn(): Boolean = settings.getBoolean(KEY_KEEP_SIGNED_IN, true)

    override fun setBiometricEnabled(value: Boolean) {
        settings.putBoolean(KEY_BIOMETRIC, value)
    }

    override fun isBiometricEnabled(): Boolean = settings.getBoolean(KEY_BIOMETRIC, false)

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_KEEP_SIGNED_IN = "keep_signed_in"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
