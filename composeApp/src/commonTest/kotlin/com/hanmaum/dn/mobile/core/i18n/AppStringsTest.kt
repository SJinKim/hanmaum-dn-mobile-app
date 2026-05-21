package com.hanmaum.dn.mobile.core.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppStringsTest {

    @Test
    fun `EnStrings has non-blank values for all keys`() {
        val s = EnStrings
        assertTrue(s.retry.isNotBlank())
        assertTrue(s.save.isNotBlank())
        assertTrue(s.cancel.isNotBlank())
        assertTrue(s.navHome.isNotBlank())
        assertTrue(s.navAlbum.isNotBlank())
        assertTrue(s.profileLogout.isNotBlank())
        assertTrue(s.selectLanguage.isNotBlank())
        assertTrue(s.list.isNotBlank())
        assertEquals(13, s.months.size)
        assertTrue(s.months[1].isNotBlank())
    }

    @Test
    fun `KoStrings has non-blank values for all keys`() {
        val s = KoStrings
        assertTrue(s.retry.isNotBlank())
        assertTrue(s.save.isNotBlank())
        assertTrue(s.back.isNotBlank())
        assertTrue(s.navHome.isNotBlank())
        assertTrue(s.selectLanguage.isNotBlank())
        assertTrue(s.list.isNotBlank())
        assertTrue(s.checkStatus.isNotBlank())
        assertEquals("년", s.yearSuffix)
        assertEquals(13, s.months.size)
        assertTrue(s.months[1].isNotBlank())
    }

    @Test
    fun `DeStrings has non-blank values for all keys`() {
        val s = DeStrings
        assertTrue(s.retry.isNotBlank())
        assertTrue(s.save.isNotBlank())
        assertTrue(s.back.isNotBlank())
        assertTrue(s.navHome.isNotBlank())
        assertTrue(s.selectLanguage.isNotBlank())
        assertTrue(s.list.isNotBlank())
        assertTrue(s.checkStatus.isNotBlank())
        assertEquals("", s.yearSuffix)
        assertEquals(13, s.months.size)
        assertTrue(s.months[1].isNotBlank())
    }

    @Test
    fun `AppLocale entries cover EN KO DE`() {
        val codes = AppLocale.entries.map { it.code }
        assertTrue(codes.contains("en"))
        assertTrue(codes.contains("ko"))
        assertTrue(codes.contains("de"))
    }

    @Test
    fun `AppLocale nativeNames are non-blank`() {
        AppLocale.entries.forEach { locale ->
            assertTrue(locale.nativeName.isNotBlank(), "nativeName blank for $locale")
        }
    }
}
