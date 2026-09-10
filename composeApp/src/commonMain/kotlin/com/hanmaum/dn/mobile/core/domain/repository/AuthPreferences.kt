package com.hanmaum.dn.mobile.core.domain.repository

/**
 * The two login conveniences the member controls themselves.
 *
 * Face ID here is not a UI gate over stored credentials — that was the design
 * before #200. What it guards is a refresh token sealed in the OS vault, which
 * the system releases only against a live biometric match. That is why signing
 * out no longer switches it off: the sealed token belongs to the member, and
 * only their face can spend it (#218).
 */
interface AuthPreferences {
    fun isKeepSignedInEnabled(): Boolean
    fun setKeepSignedInEnabled(value: Boolean)

    fun isBiometricEnabled(): Boolean

    /** Switching it off also forgets [biometricMemberId] — the two belong together. */
    fun setBiometricEnabled(value: Boolean)

    /**
     * Whose session the sealed secret opens, as `publicId`.
     *
     * Needed because the arming now survives a sign-out: without it, a second
     * member signing in with their password on the same device would find a
     * Face ID button that hands them the first member's session.
     */
    fun biometricMemberId(): String?
    fun setBiometricMemberId(id: String?)

    /** Whoever signed in last. Arming copies this into [biometricMemberId]. */
    fun signedInMemberId(): String?
    fun setSignedInMemberId(id: String?)
}
