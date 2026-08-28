package com.hanmaum.dn.mobile.core.domain.repository

/**
 * Records which event RSVPs the member has already handled (checked in OR dismissed),
 * so the sheet does not re-prompt for them. The backend exposes no "did I RSVP?" query,
 * so this local record is the only suppression source across app launches. Events are
 * one-off, so the key is the RSVP publicId alone.
 */
interface EventRsvpPreferences {
    fun isHandled(publicId: String): Boolean
    fun markHandled(publicId: String)
}
