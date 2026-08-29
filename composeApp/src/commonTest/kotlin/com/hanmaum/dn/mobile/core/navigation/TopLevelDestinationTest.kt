package com.hanmaum.dn.mobile.core.navigation

import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.DeStrings
import com.hanmaum.dn.mobile.core.i18n.EnStrings
import com.hanmaum.dn.mobile.core.i18n.KoStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopLevelDestinationTest {

    @Test
    fun `all contains exactly 4 entries`() {
        // Profile left the tab bar in the v2 redesign; it is reached through
        // the avatar in the Home header instead.
        assertEquals(4, TopLevelDestination.all.size)
    }

    @Test
    fun `all lists destinations in Home News Calendar Album order`() {
        val entries = TopLevelDestination.all
        assertTrue(entries[0] is TopLevelDestination.Home)
        assertTrue(entries[1] is TopLevelDestination.News)
        assertTrue(entries[2] is TopLevelDestination.Calendar)
        assertTrue(entries[3] is TopLevelDestination.Album)
    }

    @Test
    fun `every destination has its own icon`() {
        val icons = TopLevelDestination.all.map { it.icon }
        assertEquals(icons.size, icons.distinct().size, "Two tabs share an icon")
    }

    @Test
    fun `every locale labels every tab`() {
        // Labels moved out of the destination and into AppStrings, so a new
        // locale that forgets a tab has to fail here.
        listOf<AppStrings>(EnStrings, KoStrings, DeStrings).forEach { s ->
            listOf(s.navHome, s.navNews, s.navCalendar, s.navAlbum).forEach { label ->
                assertTrue(label.isNotBlank(), "Blank tab label in $s")
            }
        }
    }

    @Test
    fun `Home routeClass is HomeRoute`() {
        assertEquals(HomeRoute::class, TopLevelDestination.Home.routeClass)
    }

    @Test
    fun `News routeClass is AnnouncementListRoute`() {
        assertEquals(AnnouncementListRoute::class, TopLevelDestination.News.routeClass)
    }

    @Test
    fun `Calendar routeClass is CalendarRoute`() {
        assertEquals(CalendarRoute::class, TopLevelDestination.Calendar.routeClass)
    }

    @Test
    fun `Album routeClass is AlbumsRoute`() {
        assertEquals(AlbumsRoute::class, TopLevelDestination.Album.routeClass)
    }
}
