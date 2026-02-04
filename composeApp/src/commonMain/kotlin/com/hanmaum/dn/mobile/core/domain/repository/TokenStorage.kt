package com.hanmaum.dn.mobile.core.domain.repository

interface TokenStorage {
    fun saveAccessToken(token: String)
    fun getAccessToken(): String?
    fun saveRefreshToken(token: String?)
    fun getRefreshToken(): String?
    fun clear()
}