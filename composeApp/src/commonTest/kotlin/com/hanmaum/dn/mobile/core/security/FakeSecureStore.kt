package com.hanmaum.dn.mobile.core.security

/**
 * Stands in for the Keychain / Keystore-backed store.
 *
 * [survivesUninstall] mirrors the property that makes the fresh-install wipe
 * necessary: on iOS a Keychain item outlives the app that wrote it.
 */
class FakeSecureStore(
    private val values: MutableMap<String, String> = mutableMapOf(),
) : SecureStore {

    override fun putString(key: String, value: String) { values[key] = value }

    override fun getString(key: String): String? = values[key]

    override fun remove(key: String) { values.remove(key) }

    /** What a Keychain still holds after the app was deleted and reinstalled. */
    fun survivesUninstall(): FakeSecureStore = FakeSecureStore(values)
}
