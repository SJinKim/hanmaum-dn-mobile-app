package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.SecureStore
import com.russhwolf.settings.Settings

/**
 * Session tokens, held where the platform encrypts them.
 *
 * They used to live in plain `Settings` — NSUserDefaults and SharedPreferences —
 * which put the refresh token in readable storage for the whole of a "keep me
 * signed in" session. That undercut the Face ID vault completely: anyone able to
 * read the app directory had no reason to attack the sealed copy when an
 * identical token lay unprotected beside it (#222).
 *
 * [settings] stays for one job that is not the tokens: carrying the values of an
 * older install across to the [secureStore]. Emptying the store on a fresh
 * installation belongs to [com.hanmaum.dn.mobile.core.security.InstallationGuard],
 * which does the same for the Face ID vault (#225).
 */
class TokenStorageImpl(
    private val secureStore: SecureStore,
    private val settings: Settings,
) : TokenStorage {

    init {
        migrateFromPlainSettings()
    }

    override fun saveAccessToken(token: String) {
        secureStore.putString(KEY_ACCESS, token)
    }

    override fun getAccessToken(): String? = secureStore.getString(KEY_ACCESS)

    override fun saveRefreshToken(token: String?) {
        if (token != null) secureStore.putString(KEY_REFRESH, token) else secureStore.remove(KEY_REFRESH)
    }

    override fun getRefreshToken(): String? = secureStore.getString(KEY_REFRESH)

    override fun clear() {
        // Session-only teardown. The member's preferences are not session state
        // and live in AuthPreferences, so nothing here touches them: Face ID
        // sign-in must survive a session expiry — that is exactly when the login
        // screen needs it to offer the prompt.
        secureStore.remove(KEY_ACCESS)
        secureStore.remove(KEY_REFRESH)
    }

    /** Carries a signed-in member across the update instead of signing them out. */
    private fun migrateFromPlainSettings() {
        settings.getStringOrNull(KEY_ACCESS)?.let {
            secureStore.putString(KEY_ACCESS, it)
            settings.remove(KEY_ACCESS)
        }
        settings.getStringOrNull(KEY_REFRESH)?.let {
            secureStore.putString(KEY_REFRESH, it)
            settings.remove(KEY_REFRESH)
        }
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
