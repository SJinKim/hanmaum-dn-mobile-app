package com.hanmaum.dn.mobile.features.verse.domain.repository

import com.hanmaum.dn.mobile.features.verse.domain.model.DailyVerse
import com.hanmaum.dn.mobile.features.verse.domain.model.WeeklyVerse

interface VerseRepository {
    /**
     * The passage for today, or `null` when there is none.
     *
     * A null result is a normal outcome, not an error: the plan has no
     * quiet-time entry on Sundays, and none at all for a year the church has
     * not written yet. Both cases hide the card rather than showing a failure.
     */
    suspend fun getTodayVerse(): Result<DailyVerse?>

    /**
     * This week's memory verse, or `null` when none is set.
     *
     * Null is normal, not an error: the verse is chosen by an admin, and until
     * someone does that for the running week there is nothing to recite. The
     * card hides itself rather than showing an empty frame.
     */
    suspend fun getWeeklyVerse(): Result<WeeklyVerse?>
}
