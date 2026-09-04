package com.hanmaum.dn.mobile.features.attendance.domain.model

/**
 * The counters behind the 이번 달 / 올해 / 출석률 tiles.
 *
 * [rate] is the server's own 0..1 fraction rather than something recomputed
 * here. Its denominator is occurrences scheduled *up to today*, not the whole
 * year — dividing by all 52 Sundays would report a member with perfect
 * attendance in January at 8%. Recomputing it client-side from
 * [yearAttended] and [yearToDateTotal] would give the same answer today and
 * drift the day the server changes what counts.
 */
data class AttendanceSummary(
    val monthAttended: Int,
    val monthTotal: Int,
    val yearAttended: Int,
    val yearToDateTotal: Int,
    val rate: Double,
) {
    /** `rate` as whole percent, for the 출석률 tile. */
    val ratePercent: Int get() = (rate * 100).toInt()
}

/** One scheduled occurrence and whether the member made it. */
data class AttendanceEntry(
    val definitionPublicId: String,
    val definitionTitle: String,
    /** ISO date, "2026-09-04". */
    val date: String,
    val checkedIn: Boolean,
)

/** The 최근 출석 list over the range the server resolved. */
data class AttendanceHistory(
    val from: String,
    val to: String,
    val entries: List<AttendanceEntry>,
)
