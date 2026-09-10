package com.hanmaum.dn.mobile.features.verse.domain.model

import kotlinx.datetime.LocalDate

/**
 * The memory verse the church is on, as the server last answered.
 *
 * Unlike [DailyVerse] this one carries its text: a memory verse is a handful of
 * verses, and reciting it is the point of the card. The reference stays Korean
 * only — the text itself is Korean, so an English reference under it would name
 * a passage the member cannot read here anyway (the Figma card shows one).
 *
 * Not necessarily *this* week's verse: when nothing is published for the running
 * week the server answers with the most recent one it has. [weekStart]/[weekEnd]
 * say which week it belongs to, and the card shows that span for exactly that
 * reason — an older verse is fine, an older verse passed off as current is not.
 */
data class WeeklyVerse(
    /** e.g. `시편 23:1`. */
    val reference: String,
    /** The passage itself, already joined into one paragraph. */
    val text: String,
    /** Translation the text is taken from, e.g. `개역개정`. May be empty. */
    val translation: String,
    /** Sunday the verse's week starts on. Null when the server sent no span. */
    val weekStart: LocalDate? = null,
    /** Saturday the verse's week ends on. Null when the server sent no span. */
    val weekEnd: LocalDate? = null,
)
