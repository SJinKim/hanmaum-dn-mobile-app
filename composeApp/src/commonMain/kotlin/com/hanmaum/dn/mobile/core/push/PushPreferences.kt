package com.hanmaum.dn.mobile.core.push

import com.russhwolf.settings.Settings

interface PushPreferences {
    fun isPromptDismissed(): Boolean
    fun setPromptDismissed(dismissed: Boolean)
}

class PushPreferencesImpl(private val settings: Settings) : PushPreferences {
    override fun isPromptDismissed(): Boolean = settings.getBoolean(KEY, false)
    override fun setPromptDismissed(dismissed: Boolean) = settings.putBoolean(KEY, dismissed)
    private companion object {
        const val KEY = "push_prompt_dismissed"
    }
}
