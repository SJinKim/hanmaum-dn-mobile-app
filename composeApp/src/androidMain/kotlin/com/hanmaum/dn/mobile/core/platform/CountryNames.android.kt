package com.hanmaum.dn.mobile.core.platform

import java.util.Locale

actual fun localizedCountryName(iso: String, languageTag: String): String? {
    val region = runCatching { Locale.Builder().setRegion(iso).build() }.getOrNull() ?: return null
    val name = region.getDisplayCountry(Locale.forLanguageTag(languageTag))
    // An unknown region comes back as the code itself.
    return name.takeIf { it.isNotBlank() && !it.equals(iso, ignoreCase = true) }
}
