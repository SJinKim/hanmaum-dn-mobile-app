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

    @Test
    fun toE164DropsTheTrunkZeroOfANationalNumber() {
        assertEquals("+491512345678", PhoneNumber.toE164("49", "0151 2345678"))
        assertEquals("+821012345678", PhoneNumber.toE164("82", "010 1234 5678"))
        assertEquals("+821012345678", PhoneNumber.toE164("82", "10 1234 5678"))
    }

    @Test
    fun toE164KeepsTheLeadingZeroWhereItIsPartOfTheNumber() {
        // Italy and San Marino dial the 0 after the country code.
        assertEquals("+39061234", PhoneNumber.toE164("39", "06 1234"))
        assertEquals("+3780549123", PhoneNumber.toE164("378", "0549 123"))
    }

    @Test
    fun germanyAndKoreaArePinnedFirstAndSecond() {
        assertEquals(listOf("DE", "KR"), Countries.pinned.map { it.iso })
        val rest = Countries.unpinnedSortedBy { it.name }
        assertTrue(rest.none { it.iso in Countries.PINNED_ISOS })
        assertEquals(Countries.all.size, rest.size + Countries.pinned.size)
        assertEquals(rest.map { it.name.lowercase() }.sorted(), rest.map { it.name.lowercase() })
    }

    @Test
    fun aSearchFiltersThePinnedCountriesToo() {
        val korean = mapOf("DE" to "독일", "KR" to "대한민국")
        val nameOf: (Country) -> String = { korean[it.iso] ?: it.name }
        assertEquals(listOf("KR"), Countries.pinned.filter { Countries.matches(it, "대한", nameOf) }.map { it.iso })
        assertTrue(Countries.pinned.none { Countries.matches(it, "france", nameOf) })
        assertTrue(Countries.matches(Countries.byIsoOrDefault("DE"), "germany", nameOf))
    }

    @Test
    fun splitE164ReadsTheDialCodeBack() {
        assertEquals("DE" to "1631234567", PhoneNumber.splitE164("+491631234567"))
        assertEquals("KR" to "1012345678", PhoneNumber.splitE164("+821012345678"))
        assertEquals("US" to "2025550123", PhoneNumber.splitE164("+12025550123"))
        assertEquals("RU" to "9161234567", PhoneNumber.splitE164("+79161234567"))
    }

    @Test
    fun splitE164PrefersTheLongestDialCode() {
        // +353 must not be read as a shorter code that happens to prefix it.
        assertEquals("IE" to "871234567", PhoneNumber.splitE164("+353871234567"))
    }

    @Test
    fun splitE164ReadsALegacyNumberAsGerman() {
        assertEquals("DE" to "01631234567", PhoneNumber.splitE164("0163 1234567"))
        assertEquals("+491631234567", PhoneNumber.toE164("49", "01631234567"))
    }

    @Test
    fun splitE164IsNullWithoutDigits() {
        assertNull(PhoneNumber.splitE164(null))
        assertNull(PhoneNumber.splitE164("  "))
    }

    @Test
    fun splitE164RoundTripsThroughToE164() {
        val (iso, national) = PhoneNumber.splitE164("+491631234567")!!
        assertEquals("+491631234567", PhoneNumber.toE164(Countries.byIsoOrDefault(iso).dialCode, national))
    }
}
