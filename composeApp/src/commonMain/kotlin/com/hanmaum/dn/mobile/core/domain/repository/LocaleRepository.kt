package com.hanmaum.dn.mobile.core.domain.repository

import com.hanmaum.dn.mobile.core.i18n.AppLocale

interface LocaleRepository {
    /** Null means this installation has no supported, explicit language choice. */
    fun getSelectedLocale(): AppLocale?
    fun getLocale(): AppLocale
    fun setLocale(locale: AppLocale)
}
