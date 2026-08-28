package com.hanmaum.dn.mobile.core.domain.repository

import com.hanmaum.dn.mobile.core.domain.model.ThemeMode

interface ThemeRepository {
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
}
