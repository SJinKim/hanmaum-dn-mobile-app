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
        assertTrue(s.ministryListTitle.isNotBlank())
        assertTrue(s.ministryAbout.isNotBlank())
        assertTrue(s.ministryContact.isNotBlank())
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
        assertTrue(s.ministryListTitle.isNotBlank())
        assertEquals("소개", s.ministryAbout)
        assertEquals("일정", s.ministrySchedule)
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
        assertTrue(s.ministryListTitle.isNotBlank())
        assertEquals("Über uns", s.ministryAbout)
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

    @Test
    fun `time together renders all four branches in every language`() {
        // The profile tile (#114) is the visible output of membershipDuration.
        // Each language breaks the units differently so each gets its own row.
        listOf(EnStrings, KoStrings, DeStrings).forEach { s ->
            assertTrue(s.profileTimeTogether.isNotBlank())
            listOf(
                s.profileTimeTogetherValue(4, 6),  // years and months
                s.profileTimeTogetherValue(4, 0),  // exact anniversary
                s.profileTimeTogetherValue(0, 8),  // first year
                s.profileTimeTogetherValue(0, 0),  // brand new member
            ).forEach { assertTrue(it.isNotBlank()) }
        }
    }

    @Test
    fun `time together drops the month part on an exact anniversary`() {
        assertEquals("4년", KoStrings.profileTimeTogetherValue(4, 0))
        assertEquals("4y", EnStrings.profileTimeTogetherValue(4, 0))
        assertEquals("4 J.", DeStrings.profileTimeTogetherValue(4, 0))
    }

    @Test
    fun `time together says something other than zero for a new member`() {
        // "0개월" would read as an error; a fresh member gets a word instead.
        listOf(EnStrings, KoStrings, DeStrings).forEach { s ->
            val v = s.profileTimeTogetherValue(0, 0)
            assertTrue(!v.contains("0"), "expected no zero in \"$v\"")
        }
    }
}
