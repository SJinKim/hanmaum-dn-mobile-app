package com.hanmaum.dn.mobile.features.member.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.periodUntil

/**
 * How long a member has belonged to the church, as whole years and the
 * leftover whole months — the granularity the profile tile shows.
 */
data class MembershipDuration(val years: Int, val months: Int)

/**
 * Derives [MembershipDuration] from the member's registration date.
 *
 * `registrationDate` is the wire field (an ISO date, or null: it is nullable
 * on the server too, so an older record simply has none). Kept as a pure
 * function taking [today] rather than reading a clock, so every boundary is
 * testable and the composable stays free of time logic.
 *
 * Returns null when there is nothing honest to show — no date, an unparseable
 * one, or a date in the future — and the caller renders a dash instead of a
 * fabricated zero.
 */
fun membershipDuration(registrationDate: String?, today: LocalDate): MembershipDuration? {
    val start = registrationDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    if (start > today) return null
    val period = start.periodUntil(today)
    return MembershipDuration(years = period.years, months = period.months)
}
