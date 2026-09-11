package com.hanmaum.dn.mobile.core.security

import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.russhwolf.settings.Settings

/**
 * Makes a fresh installation start with nothing.
 *
 * Both secure stores outlive the app on iOS: a Keychain item is not deleted with
 * the app that wrote it. Plain settings are. A missing marker in the plain
 * settings therefore identifies an installation that has never run — and
 * everything the Keychain still holds belongs to one that is gone.
 *
 * Without this, deleting the app and installing it again could leave Face ID
 * armed with a sealed token nobody on this installation ever put there, and the
 * login screen would offer a sign-in the member never set up (#225).
 *
 * One marker, one owner. It is the same key `TokenStorageImpl` used in 0.8.4, so
 * a device that already ran that build is recognised as one that has run before
 * and keeps its Face ID setup.
 */
class InstallationGuard(
    private val settings: Settings,
    private val secureStore: SecureStore,
    private val vault: BiometricVault,
    private val authPreferences: AuthPreferences,
) {
    fun enforce() {
        if (settings.getBoolean(KEY_INSTALLED, false)) return

        secureStore.remove(KEY_ACCESS)
        secureStore.remove(KEY_REFRESH)
        // The sealed token and the switch go together: leaving either behind is
        // what produced a Face ID button on an installation that never armed one.
        vault.clear()
        authPreferences.setBiometricEnabled(false)

        settings.putBoolean(KEY_INSTALLED, true)
    }

    private companion object {
        const val KEY_INSTALLED = "token_store_installed"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
