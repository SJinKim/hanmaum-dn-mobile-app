package com.hanmaum.dn.mobile.features.verse.domain.model

/**
 * The passage the church reads on a given day.
 *
 * Home shows the reference only — a quiet-time passage runs 8–25 verses and
 * does not fit on a card — plus a link out to the church's reading page.
 * The reference arrives pre-rendered in both languages because resolving book
 * names needs the upstream book table, which lives behind the server's API key
 * (see hanmaum-dn-server#115).
 */
data class DailyVerse(
    /** e.g. `신명기 3:1-11`. Empty if the server could not render it. */
    val referenceKo: String,
    /** e.g. `Deuteronomy 3:1-11`. Empty if the server could not render it. */
    val referenceEn: String,
    /** Translation the reference refers to, e.g. `개역개정`. May be empty. */
    val translation: String,
    /** Deep link onto the day's passage; null when the server omits it. */
    val sourceUrl: String?,
)
