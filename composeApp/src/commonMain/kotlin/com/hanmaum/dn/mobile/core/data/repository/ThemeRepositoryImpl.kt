package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.domain.repository.ThemeRepository
import com.russhwolf.settings.Settings

class ThemeRepositoryImpl(private val settings: Settings) : ThemeRepository {

    override fun getThemeMode(): ThemeMode =
        ThemeMode.entries.find { it.name == settings.getStringOrNull(KEY) } ?: ThemeMode.SYSTEM

    override fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY, mode.name)
    }

    private companion object {
        const val KEY = "app_theme_mode"
    }
}
