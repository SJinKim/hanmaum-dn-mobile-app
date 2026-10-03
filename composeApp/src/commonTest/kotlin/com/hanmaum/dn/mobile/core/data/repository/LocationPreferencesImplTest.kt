package com.hanmaum.dn.mobile.core.data.repository

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationPreferencesImplTest {

    @Test
    fun sharingIsEnabledByDefault() {
        // Opt-out since #146: the check-in sends the location unless the
        // member turned it off in the profile.
        val preferences = LocationPreferencesImpl(MapSettings())

        assertTrue(preferences.isSharingEnabled())
    }

    @Test
    fun sharingChoiceSurvivesRepositoryRecreation() {
        val settings = MapSettings()
        LocationPreferencesImpl(settings).setSharingEnabled(false)

        val recreatedPreferences = LocationPreferencesImpl(settings)

        assertFalse(recreatedPreferences.isSharingEnabled())
    }

    @Test
    fun sharingCanBeDisabledAgain() {
        val settings = MapSettings()
        val preferences = LocationPreferencesImpl(settings)
        preferences.setSharingEnabled(true)

        preferences.setSharingEnabled(false)

        assertFalse(LocationPreferencesImpl(settings).isSharingEnabled())
    }
}
