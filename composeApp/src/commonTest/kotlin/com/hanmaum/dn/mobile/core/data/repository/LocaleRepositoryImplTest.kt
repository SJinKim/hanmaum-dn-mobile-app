package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleRepositoryImplTest {

    private fun repo(settings: MapSettings = MapSettings()) = LocaleRepositoryImpl(settings)

    @Test
    fun `getLocale returns EN when no value stored`() {
        assertEquals(AppLocale.EN, repo().getLocale())
    }

    @Test
    fun `setLocale persists and getLocale returns the same value`() {
        val settings = MapSettings()
        val r = repo(settings)
        r.setLocale(AppLocale.KO)
        assertEquals(AppLocale.KO, repo(settings).getLocale())
    }

    @Test
    fun `setLocale DE round-trips correctly`() {
        val settings = MapSettings()
        val r = repo(settings)
        r.setLocale(AppLocale.DE)
        assertEquals(AppLocale.DE, repo(settings).getLocale())
    }

    @Test
    fun `getLocale returns EN for unknown stored value`() {
        val settings = MapSettings("app_locale" to "xx")
        assertEquals(AppLocale.EN, repo(settings).getLocale())
    }
}
