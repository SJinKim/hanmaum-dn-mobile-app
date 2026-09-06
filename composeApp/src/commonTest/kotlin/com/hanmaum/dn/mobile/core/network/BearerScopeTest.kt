package com.hanmaum.dn.mobile.core.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val BACKEND = "api.hanmaum-dn.de"

/**
 * The bearer plugin is host-scoped on purpose. Widening it to a path check is
 * the mistake CLAUDE.md §6.4 names: the church token would then be attached to
 * Google Calendar and S3, which leaks it and breaks those calls outright.
 */
class BearerScopeTest {

    @Test
    fun `the backend gets the token`() {
        assertTrue(shouldSendBearer("api.hanmaum-dn.de", "/api/v1/announcements", BACKEND))
    }

    @Test
    fun `a relative url is still the backend`() {
        assertTrue(shouldSendBearer("", "/announcements", BACKEND))
    }

    @Test
    fun `google calendar never gets the token`() {
        assertFalse(
            shouldSendBearer("www.googleapis.com", "/calendar/v3/calendars/x/events", BACKEND),
        )
    }

    @Test
    fun `presigned s3 uploads never get the token`() {
        assertFalse(shouldSendBearer("hanmaum-dn.s3.eu-central-1.amazonaws.com", "/photos/1.jpg", BACKEND))
    }

    @Test
    fun `keycloak never gets the token`() {
        assertFalse(
            shouldSendBearer("auth.hanmaum-dn.de", "/realms/dn/protocol/openid-connect/token", BACKEND),
        )
    }

    @Test
    fun `registration on the backend stays unauthenticated`() {
        assertFalse(shouldSendBearer(BACKEND, "/api/v1/members/register", BACKEND))
    }
}
