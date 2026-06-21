package com.hanmaum.dn.mobile.core.data.repository

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationPreferencesImplTest {

    @Test
    fun sharingIsDisabledByDefault() {
        val preferences = LocationPreferencesImpl(MapSettings())

        assertFalse(preferences.isSharingEnabled())
    }

    @Test
    fun sharingChoiceSurvivesRepositoryRecreation() {
        val settings = MapSettings()
        LocationPreferencesImpl(settings).setSharingEnabled(true)

        val recreatedPreferences = LocationPreferencesImpl(settings)

        assertTrue(recreatedPreferences.isSharingEnabled())
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
