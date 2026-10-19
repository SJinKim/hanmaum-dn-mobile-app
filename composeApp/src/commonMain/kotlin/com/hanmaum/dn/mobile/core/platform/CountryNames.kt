package com.hanmaum.dn.mobile.core.platform

/**
 * The name of the country [iso] (ISO 3166 alpha-2) in the language
 * [languageTag], from the platform's locale data — or null when the platform
 * does not know it, so the caller can fall back to its own name.
 */
expect fun localizedCountryName(iso: String, languageTag: String): String?
