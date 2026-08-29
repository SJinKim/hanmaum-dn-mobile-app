package com.hanmaum.dn.mobile.core.domain.repository

/**
 * The two login conveniences the member controls themselves.
 *
 * Face ID only reveals credentials that are already stored; it is a UI gate,
 * not a second factor, so it is meaningless without [isKeepSignedInEnabled].
 */
interface AuthPreferences {
    fun isKeepSignedInEnabled(): Boolean
    fun setKeepSignedInEnabled(value: Boolean)

    fun isBiometricEnabled(): Boolean
    fun setBiometricEnabled(value: Boolean)
}
