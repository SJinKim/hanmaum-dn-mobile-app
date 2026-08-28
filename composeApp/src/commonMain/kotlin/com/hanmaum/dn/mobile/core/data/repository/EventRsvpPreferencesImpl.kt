package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.EventRsvpPreferences
import com.russhwolf.settings.Settings

class EventRsvpPreferencesImpl(private val settings: Settings) : EventRsvpPreferences {
    override fun isHandled(publicId: String): Boolean =
        settings.getBoolean(key(publicId), false)

    override fun markHandled(publicId: String) =
        settings.putBoolean(key(publicId), true)

    private fun key(publicId: String) = "$PREFIX$publicId"

    private companion object {
        const val PREFIX = "event_rsvp_handled_"
    }
}
