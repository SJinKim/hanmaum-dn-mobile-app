package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.domain.repository.ThemeRepository
import com.russhwolf.settings.Settings

class ThemeRepositoryImpl(private val settings: Settings) : ThemeRepository {

    override fun getThemeMode(): ThemeMode =
        settings.getStringOrNull(KEY)
            ?.let { stored -> ThemeMode.entries.find { it.name == stored } }
            ?: ThemeMode.SYSTEM

    override fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY, mode.name)
    }

    companion object {
        private const val KEY = "theme_mode"
    }
}
