package com.hanmaum.dn.mobile.features.verse.domain.model

/**
 * The two things a member can mark off on the Home verse cards.
 *
 * Separate streaks on purpose: reading the day's passage and reciting the
 * week's verse are different practices, and one counter would let either hide
 * the other. The names match the wire enum.
 */
enum class VerseRecordKind {
    QUIET_TIME,
    RECITATION,
}
