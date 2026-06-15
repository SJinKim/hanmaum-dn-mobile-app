package com.hanmaum.dn.mobile.core.security

/** Saved login credentials, released only behind a successful biometric check. */
data class Credentials(val email: String, val password: String)

/**
 * Stores the email/password used for Face ID sign-in. Backed by [SecureStore]
 * (Keychain / AndroidKeyStore), so the password is encrypted at rest and only
 * read after the user passes a biometric prompt.
 */
class CredentialStore(private val secureStore: SecureStore) {

    fun saveCredentials(email: String, password: String) {
        secureStore.putString(KEY_EMAIL, email)
        secureStore.putString(KEY_PASSWORD, password)
    }

    fun getCredentials(): Credentials? {
        val email = secureStore.getString(KEY_EMAIL) ?: return null
        val password = secureStore.getString(KEY_PASSWORD) ?: return null
        return Credentials(email, password)
    }

    fun hasCredentials(): Boolean = secureStore.getString(KEY_EMAIL) != null

    fun clear() {
        secureStore.remove(KEY_EMAIL)
        secureStore.remove(KEY_PASSWORD)
    }

    private companion object {
        const val KEY_EMAIL = "cred_email"
        const val KEY_PASSWORD = "cred_password"
    }
}
