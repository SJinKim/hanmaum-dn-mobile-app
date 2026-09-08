package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.russhwolf.settings.Settings

class TokenStorageImpl(private val settings: Settings) : TokenStorage {

    override fun saveAccessToken(token: String) {
        settings.putString(KEY_ACCESS, token)
    }

    override fun getAccessToken(): String? = settings.getStringOrNull(KEY_ACCESS)

    override fun saveRefreshToken(token: String?) {
        if (token != null) settings.putString(KEY_REFRESH, token)
        else settings.remove(KEY_REFRESH)
    }

    override fun getRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH)

    override fun clear() {
        // Session-only teardown. The member's preferences are not session state
        // and live in AuthPreferences, so nothing here touches them: Face ID
        // sign-in must survive a session expiry — that is exactly when the login
        // screen needs it to offer the saved-credential prompt. Intentional
        // teardown (explicit logout, REJECTED/DELETED account) turns the flag off
        // through AuthPreferences.setBiometricEnabled(false).
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
