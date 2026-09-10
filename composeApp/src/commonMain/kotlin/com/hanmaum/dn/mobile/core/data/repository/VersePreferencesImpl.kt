package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.RememberedWeeklyVerse
import com.hanmaum.dn.mobile.core.domain.repository.VersePreferences
import com.russhwolf.settings.Settings
import kotlinx.datetime.LocalDate

class VersePreferencesImpl(private val settings: Settings) : VersePreferences {

    override fun rememberedWeekly(): RememberedWeeklyVerse? {
        val reference = settings.getStringOrNull(KEY_REFERENCE)?.takeIf { it.isNotBlank() } ?: return null
        return RememberedWeeklyVerse(
            reference = reference,
            weekStart = settings.getStringOrNull(KEY_WEEK_START)?.toLocalDateOrNull(),
            weekEnd = settings.getStringOrNull(KEY_WEEK_END)?.toLocalDateOrNull(),
        )
    }

    override fun rememberWeekly(verse: RememberedWeeklyVerse) {
        settings.putString(KEY_REFERENCE, verse.reference)
        // Removed rather than blanked: a week the server stopped sending must not
        // leave last week's dates standing under a newer reference.
        verse.weekStart.write(KEY_WEEK_START)
        verse.weekEnd.write(KEY_WEEK_END)
    }

    private fun LocalDate?.write(key: String) {
        if (this == null) settings.remove(key) else settings.putString(key, toString())
    }

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

    private companion object {
        const val KEY_REFERENCE = "verse.weekly.reference"
        const val KEY_WEEK_START = "verse.weekly.weekStart"
        const val KEY_WEEK_END = "verse.weekly.weekEnd"
    }
}
