package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import io.ktor.client.HttpClient

// TODO: Simpelste Version (Hält nur solange App offen ist) - Später durch echte Datenbank/Preferences ersetzen!
class TokenStorageImpl: TokenStorage {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    override fun saveAccessToken(token: String) {
        accessToken = token
    }

    override fun getAccessToken(): String? = accessToken

    override fun saveRefreshToken(token: String?) {
        refreshToken = token
    }

    override fun getRefreshToken(): String? = refreshToken

    override fun clear() {
        accessToken = null
        refreshToken = null
    }
}