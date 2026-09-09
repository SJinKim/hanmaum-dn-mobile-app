package com.hanmaum.dn.mobile.features.verse.domain.model

/**
 * Which of the three things `/verses/today` is telling us.
 *
 * The server decides, the app words it. An empty payload used to have two
 * causes — a Sunday, and a gap in the plan at the turn of the year — and the
 * client cannot tell them apart from the weekday alone: a January gap on a
 * Tuesday would otherwise render as a Sunday service.
 */
enum class DailyVerseState {
    /** The reading plan has a passage for today. */
    PASSAGE,

    /** Sunday: the verses come from the sermon, so there is no planned passage. */
    SUNDAY_SERVICE,

    /** A gap in the plan. Nothing honest to show, so the card stays hidden. */
    NO_PLAN,

    ;

    companion object {
        /**
         * Unknown values fall back to [NO_PLAN] — hiding the card is the safe
         * reading if the server ever grows a state this build does not know.
         */
        fun fromWire(value: String?): DailyVerseState =
            entries.firstOrNull { it.name == value } ?: NO_PLAN
    }
}
