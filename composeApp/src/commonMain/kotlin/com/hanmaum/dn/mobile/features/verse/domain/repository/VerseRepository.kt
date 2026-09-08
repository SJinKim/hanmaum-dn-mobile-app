package com.hanmaum.dn.mobile.features.verse.domain.repository

import com.hanmaum.dn.mobile.features.verse.domain.model.DailyVerse

interface VerseRepository {
    /**
     * The passage for today, or `null` when there is none.
     *
     * A null result is a normal outcome, not an error: the plan has no
     * quiet-time entry on Sundays, and none at all for a year the church has
     * not written yet. Both cases hide the card rather than showing a failure.
     */
    suspend fun getTodayVerse(): Result<DailyVerse?>
}
