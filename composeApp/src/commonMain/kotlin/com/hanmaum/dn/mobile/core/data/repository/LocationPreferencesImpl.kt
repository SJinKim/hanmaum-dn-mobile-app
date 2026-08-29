package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.russhwolf.settings.Settings

class LocationPreferencesImpl(private val settings: Settings) : LocationPreferences {

    override fun isSharingEnabled(): Boolean = settings.getBoolean(KEY_ENABLED, false)

    override fun setSharingEnabled(value: Boolean) = settings.putBoolean(KEY_ENABLED, value)

    override fun isPromptDismissed(): Boolean = settings.getBoolean(KEY_DISMISSED, false)

    override fun setPromptDismissed(value: Boolean) = settings.putBoolean(KEY_DISMISSED, value)

    override fun clear() {
        settings.remove(KEY_ENABLED)
        settings.remove(KEY_DISMISSED)
    }

    private companion object {
        const val KEY_ENABLED = "location_sharing_enabled"
        const val KEY_DISMISSED = "location_prompt_dismissed"
    }
}
