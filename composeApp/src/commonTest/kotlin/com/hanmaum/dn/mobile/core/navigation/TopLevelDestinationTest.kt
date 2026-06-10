package com.hanmaum.dn.mobile.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TopLevelDestinationTest {

    @Test
    fun `all contains exactly 5 entries`() {
        assertEquals(5, TopLevelDestination.all.size)
    }

    @Test
    fun `all entries have non-blank labels`() {
        TopLevelDestination.all.forEach { dest ->
            assertTrue(dest.label.isNotBlank(), "Expected non-blank label for $dest")
        }
    }

    @Test
    fun `all lists destinations in Home Ministry Calendar Album Profile order`() {
        val entries = TopLevelDestination.all
        assertTrue(entries[0] is TopLevelDestination.Home)
        assertTrue(entries[1] is TopLevelDestination.Ministry)
        assertTrue(entries[2] is TopLevelDestination.Calendar)
        assertTrue(entries[3] is TopLevelDestination.Album)
        assertTrue(entries[4] is TopLevelDestination.Profile)
    }

    @Test
    fun `Home routeClass is HomeRoute`() {
        assertEquals(HomeRoute::class, TopLevelDestination.Home.routeClass)
    }

    @Test
    fun `Ministry routeClass is MinistryListRoute`() {
        assertEquals(MinistryListRoute::class, TopLevelDestination.Ministry.routeClass)
    }

    @Test
    fun `Calendar routeClass is CalendarRoute`() {
        assertEquals(CalendarRoute::class, TopLevelDestination.Calendar.routeClass)
    }
}
