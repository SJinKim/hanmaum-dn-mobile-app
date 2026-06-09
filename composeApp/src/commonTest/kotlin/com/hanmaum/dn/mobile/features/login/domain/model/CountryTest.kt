package com.hanmaum.dn.mobile.features.login.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountryTest {

    @Test
    fun isoToFlagBuildsRegionalIndicatorPair() {
        // 🇩🇪 = U+1F1E9 U+1F1EA
        assertEquals("🇩🇪", isoToFlag("DE"))
        assertEquals("🇰🇷", isoToFlag("KR"))
    }

    @Test
    fun byIsoOrDefaultFallsBackToGermany() {
        assertEquals("DE", Countries.byIsoOrDefault(null).iso)
        assertEquals("DE", Countries.byIsoOrDefault("ZZ").iso)
        assertEquals("KR", Countries.byIsoOrDefault("kr").iso)
    }

    @Test
    fun searchMatchesNameAndDialCode() {
        assertTrue(Countries.search("korea").any { it.iso == "KR" })
        assertTrue(Countries.search("+49").any { it.iso == "DE" })
        assertTrue(Countries.search("49").any { it.iso == "DE" })
        assertEquals(Countries.all.size, Countries.search("  ").size)
    }

    @Test
    fun listHasNoDuplicateIsoCodes() {
        assertEquals(Countries.all.size, Countries.all.map { it.iso }.toSet().size)
    }

    @Test
    fun toE164CombinesDialCodeAndDigitsOnly() {
        assertEquals("+49170123", PhoneNumber.toE164("49", " 170 123 "))
        assertEquals("+8210", PhoneNumber.toE164("82", "abc1-0"))
        assertNull(PhoneNumber.toE164("49", "   "))
        assertNull(PhoneNumber.toE164("49", ""))
    }
}
