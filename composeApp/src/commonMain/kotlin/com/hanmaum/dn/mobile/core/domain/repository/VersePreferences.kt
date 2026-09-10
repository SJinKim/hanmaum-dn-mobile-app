package com.hanmaum.dn.mobile.core.domain.repository

import kotlinx.datetime.LocalDate

/**
 * The last memory verse this device saw, without its text.
 *
 * Reference and week are all the card needs to say "the most recent one was
 * this, and it belonged to that week". The text is deliberately not kept: it
 * would be the text of a week that has passed, and showing it would make the
 * card look current when it is not.
 */
data class RememberedWeeklyVerse(
    val reference: String,
    val weekStart: LocalDate?,
    val weekEnd: LocalDate?,
)

/**
 * Remembers the weekly memory verse across launches.
 *
 * Exists so the 주간 암송 구절 card has something to show when the server has
 * nothing — no verse published anywhere, or the church's bible source is
 * unreachable. In both cases the server is out of answers and only the device
 * still knows what it last saw.
 *
 * One verse, not a history: [remember] overwrites the previous value on a fixed
 * key, so the stored data stays three short strings no matter how many weeks
 * pass, and there is nothing to prune.
 */
interface VersePreferences {
    /** The last verse successfully loaded on this device, or null on a fresh install. */
    fun rememberedWeekly(): RememberedWeeklyVerse?

    /** Replaces whatever was remembered before. */
    fun rememberWeekly(verse: RememberedWeeklyVerse)
}
