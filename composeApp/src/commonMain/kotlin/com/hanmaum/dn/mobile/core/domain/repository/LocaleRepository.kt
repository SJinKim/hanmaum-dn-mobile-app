package com.hanmaum.dn.mobile.core.domain.repository

import com.hanmaum.dn.mobile.core.i18n.AppLocale

interface LocaleRepository {
    fun getLocale(): AppLocale
    fun setLocale(locale: AppLocale)
}
