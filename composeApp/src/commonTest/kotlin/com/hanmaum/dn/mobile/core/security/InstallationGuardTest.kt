package com.hanmaum.dn.mobile.core.security

import com.hanmaum.dn.mobile.core.data.repository.AuthPreferencesImpl
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstallationGuardTest {

    private val settings = MapSettings()
    private val secureStore = FakeSecureStore()
    private val vault = FakeBiometricVault()
    private val authPreferences = AuthPreferencesImpl(settings)

    private fun guard(
        settings: MapSettings = this.settings,
        secureStore: FakeSecureStore = this.secureStore,
    ) = InstallationGuard(settings, secureStore, vault, AuthPreferencesImpl(settings))

    /** What an installation leaves behind in the stores that outlive it. */
    private suspend fun anArmedInstallation() {
        secureStore.putString("access_token", "a")
        secureStore.putString("refresh_token", "r")
        vault.seal("sealed-refresh", "t", "s", "c")
        authPreferences.setBiometricEnabled(true)
    }

    @Test
    fun aFreshInstallInheritsNothing() = runTest {
        anArmedInstallation()
        // the app was deleted: plain settings went with it, the keychain did not
        val afterReinstall = MapSettings()

        guard(settings = afterReinstall, secureStore = secureStore.survivesUninstall()).enforce()

        assertFalse(AuthPreferencesImpl(afterReinstall).isBiometricEnabled(), "nobody armed this installation")
        assertNull(vault.sealed)
        assertTrue(vault.cleared)
    }

    @Test
    fun theMarkerFromTheEarlierBuildCountsAsHavingRun() = runTest {
        // 0.8.4 set this key from TokenStorageImpl. A device that ran it is not a
        // fresh install and must not lose its Face ID setup on the next update.
        anArmedInstallation()
        settings.putBoolean("token_store_installed", true)

        guard().enforce()

        assertTrue(authPreferences.isBiometricEnabled())
        assertEquals("sealed-refresh", vault.sealed)
    }

    @Test
    fun runningTwiceChangesNothingTheSecondTime() = runTest {
        guard().enforce()
        vault.seal("armed-later", "t", "s", "c")
        authPreferences.setBiometricEnabled(true)

        guard().enforce()

        assertTrue(authPreferences.isBiometricEnabled())
        assertEquals("armed-later", vault.sealed)
    }
}
