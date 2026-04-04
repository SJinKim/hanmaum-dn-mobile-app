package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.russhwolf.settings.Settings

class TokenStorageImpl : TokenStorage {
    private val settings = Settings()

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
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}