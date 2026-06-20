package com.hanmaum.dn.mobile.features.events

import com.hanmaum.dn.mobile.core.domain.repository.EventRsvpPreferences

class FakeEventRsvpPreferences : EventRsvpPreferences {
    val handled = mutableSetOf<String>()
    override fun isHandled(publicId: String): Boolean = publicId in handled
    override fun markHandled(publicId: String) { handled += publicId }
}
