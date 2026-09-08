package com.hanmaum.dn.mobile.core.domain.repository

/**
 * The session tokens, and nothing else.
 *
 * "Keep me signed in" and "Face ID 로그인" used to live here as well, in
 * parallel to [AuthPreferences] — two stores, two key names, one meaning. The
 * 설정 screen wrote one pair and the login and splash paths read the other, so
 * neither toggle reached the code it was supposed to steer. They now live in
 * [AuthPreferences] only.
 */
interface TokenStorage {
    fun saveAccessToken(token: String)
    fun getAccessToken(): String?
    fun saveRefreshToken(token: String?)
    fun getRefreshToken(): String?
    fun clear()
}
