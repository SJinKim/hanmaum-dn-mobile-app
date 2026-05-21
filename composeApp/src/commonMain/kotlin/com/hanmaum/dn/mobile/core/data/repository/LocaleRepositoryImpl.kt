package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.LocaleRepository
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.russhwolf.settings.Settings

class LocaleRepositoryImpl(private val settings: Settings) : LocaleRepository {

    override fun getLocale(): AppLocale =
        AppLocale.entries.find { it.code == settings.getStringOrNull(KEY) } ?: AppLocale.EN

    override fun setLocale(locale: AppLocale) {
        settings.putString(KEY, locale.code)
    }

    companion object {
        private const val KEY = "app_locale"
    }
}
