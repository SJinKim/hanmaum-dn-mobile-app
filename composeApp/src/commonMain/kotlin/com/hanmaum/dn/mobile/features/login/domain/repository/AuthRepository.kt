package com.hanmaum.dn.mobile.features.login.domain.repository

import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.model.TokenResponse

interface AuthRepository {
    suspend fun login(user: String, pass: String): TokenResponse

    /**
     * Trades a refresh token for a fresh session.
     *
     * Face ID sign-in needs this as an explicit call: the Ktor auth plugin
     * refreshes only in reaction to a 401 on some other request, whereas here
     * the refresh token released by the vault *is* the sign-in.
     */
    suspend fun refresh(refreshToken: String): TokenResponse

    suspend fun register(request: RegisterRequest): Result<Unit>
}