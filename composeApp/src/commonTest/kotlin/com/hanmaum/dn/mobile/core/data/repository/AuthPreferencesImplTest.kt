package com.hanmaum.dn.mobile.core.data.repository

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthPreferencesImplTest {

    @Test
    fun keepSignedInDefaultsOnAndBiometricDefaultsOff() {
        val prefs = AuthPreferencesImpl(MapSettings())

        assertTrue(prefs.isKeepSignedInEnabled(), "Staying signed in is the expected default")
        assertFalse(prefs.isBiometricEnabled(), "Biometrics must be opted into, never assumed")
    }

    @Test
    fun turningOffKeepSignedInAlsoTurnsOffBiometrics() {
        // Biometrics only unlock a session that is being kept; leaving the flag
        // on with nothing to unlock would show an armed switch that does nothing.
        val prefs = AuthPreferencesImpl(MapSettings())
        prefs.setBiometricEnabled(true)

        prefs.setKeepSignedInEnabled(false)

        assertFalse(prefs.isBiometricEnabled())
    }

    @Test
    fun choicesSurviveRepositoryRecreation() {
        val settings = MapSettings()
        AuthPreferencesImpl(settings).setBiometricEnabled(true)

        assertTrue(AuthPreferencesImpl(settings).isBiometricEnabled())
    }
}
