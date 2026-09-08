package com.hanmaum.dn.mobile.features.verse.domain.model

/**
 * The verse the church memorises this week.
 *
 * Unlike [DailyVerse] this one carries its text: a memory verse is a handful of
 * verses, and reciting it is the point of the card. The reference stays Korean
 * only — the text itself is Korean, so an English reference under it would name
 * a passage the member cannot read here anyway (the Figma card shows one).
 */
data class WeeklyVerse(
    /** e.g. `시편 23:1`. */
    val reference: String,
    /** The passage itself, already joined into one paragraph. */
    val text: String,
    /** Translation the text is taken from, e.g. `개역개정`. May be empty. */
    val translation: String,
)
