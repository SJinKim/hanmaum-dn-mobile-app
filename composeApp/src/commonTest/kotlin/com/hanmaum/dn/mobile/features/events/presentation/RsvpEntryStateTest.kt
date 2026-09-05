package com.hanmaum.dn.mobile.features.events.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The entry used to disappear once every invitation was answered, taking the
 * only route to 행사 참석 with it. These lock the boundary: whatever the state,
 * there is always an entry — only its wording changes.
 */
class RsvpEntryStateTest {

    @Test
    fun oneUnansweredInvitationIsOpenWorkAndCarriesTheCount() {
        val state = RsvpEntryState.of(1)
        assertIs<RsvpEntryState.Open>(state)
        assertEquals(1, state.count)
    }

    @Test
    fun severalUnansweredInvitationsCarryTheirCount() {
        assertEquals(RsvpEntryState.Open(4), RsvpEntryState.of(4))
    }

    @Test
    fun everythingAnsweredIsDoneNotAbsent() {
        // The regression: this case used to render nothing at all.
        assertEquals(RsvpEntryState.AllAnswered, RsvpEntryState.of(0))
    }

    @Test
    fun nothingToAnswerReadsTheSameAsEverythingAnswered() {
        // An empty invitation list and a fully answered one are the same fact
        // to a member: nothing is waiting on them. Both keep the way in.
        assertEquals(RsvpEntryState.of(0), RsvpEntryState.AllAnswered)
    }

    @Test
    fun theStateIsNeverAbsent() {
        // Whatever the count, there is an entry to render — that is the fix.
        for (n in 0..5) {
            val state = RsvpEntryState.of(n)
            assertEquals(n > 0, state is RsvpEntryState.Open, "count $n")
        }
    }
}
