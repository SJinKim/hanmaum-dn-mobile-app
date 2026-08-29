package com.hanmaum.dn.mobile.features.events.domain.model

/** A member's answer to an event invitation. */
enum class RsvpStatus {
    GOING,
    NOT_GOING,
    MAYBE;

    companion object {
        /**
         * Maps the wire value, returning null for anything unrecognised.
         *
         * A status the server adds later must not crash an older build — an
         * unknown answer reads as "not answered yet", which at worst asks the
         * member once more.
         */
        fun fromWire(raw: String?): RsvpStatus? = entries.firstOrNull { it.name == raw }
    }
}
