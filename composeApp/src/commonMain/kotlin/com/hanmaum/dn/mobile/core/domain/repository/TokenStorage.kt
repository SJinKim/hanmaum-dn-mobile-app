package com.hanmaum.dn.mobile.core.domain.repository

interface TokenStorage {
    fun saveAccessToken(token: String)
    fun getAccessToken(): String?
    fun saveRefreshToken(token: String?)
    fun getRefreshToken(): String?
    fun clear()

    /**
     * Whether the session should survive an app restart ("Keep me signed in").
     * Defaults to true. When false, the splash screen clears the persisted
     * session on the next launch so the user has to sign in again.
     */
    fun setKeepSignedIn(value: Boolean)
    fun isKeepSignedIn(): Boolean
}