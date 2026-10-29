package com.hanmaum.dn.mobile.core.platform

import androidx.compose.runtime.Composable

/** The installed app's marketing version, independent of privacy-document versions. */
@Composable
internal expect fun rememberAppVersion(): String?

private val marketingVersion = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$""")

/** Render the numeric release line; a two-component local Apple version has patch zero. */
internal fun displayAppVersion(value: String?): String? {
    val match = marketingVersion.matchEntire(value?.trim() ?: return null) ?: return null
    return "${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3].ifEmpty { "0" }}"
}
