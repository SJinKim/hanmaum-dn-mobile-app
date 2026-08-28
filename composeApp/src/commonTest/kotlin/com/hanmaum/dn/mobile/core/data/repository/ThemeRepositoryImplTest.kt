package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeRepositoryImplTest {

    private fun repo(settings: MapSettings = MapSettings()) = ThemeRepositoryImpl(settings)

    @Test
    fun `getThemeMode returns SYSTEM when no value stored`() {
        assertEquals(ThemeMode.SYSTEM, repo().getThemeMode())
    }

    @Test
    fun `setThemeMode persists and getThemeMode returns the same value`() {
        val settings = MapSettings()
        repo(settings).setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo(settings).getThemeMode())
    }

    @Test
    fun `setThemeMode LIGHT round-trips correctly`() {
        val settings = MapSettings()
        repo(settings).setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repo(settings).getThemeMode())
    }

    @Test
    fun `getThemeMode returns SYSTEM for unknown stored value`() {
        val settings = MapSettings("theme_mode" to "PURPLE")
        assertEquals(ThemeMode.SYSTEM, repo(settings).getThemeMode())
    }
}
