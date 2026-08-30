package com.hanmaum.dn.mobile.features.events

import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RsvpStatusTest {

    @Test
    fun knownWireValuesMapToTheirStatus() {
        assertEquals(RsvpStatus.GOING, RsvpStatus.fromWire("GOING"))
        assertEquals(RsvpStatus.NOT_GOING, RsvpStatus.fromWire("NOT_GOING"))
        assertEquals(RsvpStatus.MAYBE, RsvpStatus.fromWire("MAYBE"))
    }

    @Test
    fun anUnknownStatusReadsAsUnansweredInsteadOfCrashing() {
        assertNull(RsvpStatus.fromWire("WAITLISTED"))
        assertNull(RsvpStatus.fromWire(null))
        assertNull(RsvpStatus.fromWire(""))
    }
}
