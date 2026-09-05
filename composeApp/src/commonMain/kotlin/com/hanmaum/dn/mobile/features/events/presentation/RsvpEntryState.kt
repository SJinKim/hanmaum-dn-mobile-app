package com.hanmaum.dn.mobile.features.events.presentation

/**
 * What the 행사 참석 entry on 출석 현황 says right now.
 *
 * Extracted from the screen so the boundary is testable: the entry used to
 * vanish once everything was answered, which took the only route to that area
 * with it (#163). It is now always shown and only its contents change, so the
 * interesting question — where "open work" turns into "done" — is worth a test
 * of its own rather than living inside a composable.
 */
sealed interface RsvpEntryState {
    /** At least one invitation is unanswered. */
    data class Open(val count: Int) : RsvpEntryState

    /**
     * Everything relevant has been answered — including the case where there is
     * nothing to answer at all. Both read the same to a member: there is
     * nothing waiting on them, and the way in stays open either way.
     */
    data object AllAnswered : RsvpEntryState

    companion object {
        fun of(pendingCount: Int): RsvpEntryState =
            if (pendingCount > 0) Open(pendingCount) else AllAnswered
    }
}
