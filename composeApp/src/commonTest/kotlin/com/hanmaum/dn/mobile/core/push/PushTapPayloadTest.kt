package com.hanmaum.dn.mobile.core.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushTapPayloadTest {
    @Test
    fun `full payload parses all keys`() {
        val payload = parsePushTap(
            mapOf(
                "type" to "ANNOUNCEMENT",
                "referenceType" to "ANNOUNCEMENT",
                "referencePublicId" to "a1",
                "notificationPublicId" to "n1",
            ),
        )
        assertEquals("ANNOUNCEMENT", payload?.referenceType)
        assertEquals("a1", payload?.referencePublicId)
        assertEquals("n1", payload?.notificationPublicId)
    }

    @Test
    fun `payload without our keys returns null`() {
        assertNull(parsePushTap(mapOf("google.message_id" to "x")))
    }

    @Test
    fun `partial payload keeps missing keys null`() {
        val payload = parsePushTap(mapOf("type" to "ANNOUNCEMENT"))
        assertEquals("ANNOUNCEMENT", payload?.type)
        assertNull(payload?.referencePublicId)
    }
}
