package com.hanmaum.dn.mobile.core.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode

actual fun localizedCountryName(iso: String, languageTag: String): String? =
    NSLocale(localeIdentifier = languageTag)
        .displayNameForKey(NSLocaleCountryCode, iso)
        ?.takeIf { it.isNotBlank() }
