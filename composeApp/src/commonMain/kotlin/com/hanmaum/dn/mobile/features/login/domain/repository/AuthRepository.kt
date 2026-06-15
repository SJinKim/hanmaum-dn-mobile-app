package com.hanmaum.dn.mobile.features.login.domain.repository

import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.model.TokenResponse

interface AuthRepository {
    suspend fun login(user: String, pass: String): TokenResponse
    suspend fun register(request: RegisterRequest): Result<Unit>
}