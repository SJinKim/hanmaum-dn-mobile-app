package com.hanmaum.dn.mobile.features.ministry.presentation.list

import com.hanmaum.dn.mobile.core.navigation.ParticipationRoute
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The tab the screen opens on. The tab it *returns* to is held by
 * rememberSaveable and belongs to Compose; what is testable here is the
 * fallback for a fresh entry, and that it still reads the route's own
 * constants rather than a private copy that can drift.
 */
class ParticipationTabTest {

    @Test
    fun theServeArgumentOpens사역() {
        assertEquals(TAB_SERVE_INDEX, participationTabIndex(ParticipationRoute.TAB_SERVE))
    }

    @Test
    fun theNurtureArgumentOpens양육() {
        assertEquals(TAB_NURTURE_INDEX, participationTabIndex(ParticipationRoute.TAB_NURTURE))
    }

    @Test
    fun anUnknownArgumentFallsBackTo양육() {
        // The issue asks for a clearly defined fallback for other entry points.
        assertEquals(TAB_NURTURE_INDEX, participationTabIndex("something-else"))
        assertEquals(TAB_NURTURE_INDEX, participationTabIndex(""))
    }

    @Test
    fun theRouteDefaultOpens양육() {
        // ParticipationRoute() with no argument must not land on 사역.
        assertEquals(TAB_NURTURE_INDEX, participationTabIndex(ParticipationRoute().tab))
    }

    @Test
    fun theTwoTabsAreDistinct() {
        // Guards the drift this change removed: a second TAB_SERVE constant
        // with the same value lived in the screen file, so a change to either
        // would have sent every entry to 양육 without a compile error.
        assertEquals(
            2,
            setOf(
                participationTabIndex(ParticipationRoute.TAB_SERVE),
                participationTabIndex(ParticipationRoute.TAB_NURTURE),
            ).size,
        )
    }
}
