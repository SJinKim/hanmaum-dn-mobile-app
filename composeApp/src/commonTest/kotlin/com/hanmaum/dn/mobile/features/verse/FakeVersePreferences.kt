package com.hanmaum.dn.mobile.features.verse

import com.hanmaum.dn.mobile.core.domain.repository.RememberedWeeklyVerse
import com.hanmaum.dn.mobile.core.domain.repository.VersePreferences

/** In-memory stand-in for the settings-backed store. One slot, overwritten. */
class FakeVersePreferences(
    private var remembered: RememberedWeeklyVerse? = null,
) : VersePreferences {

    var writes: Int = 0
        private set

    override fun rememberedWeekly(): RememberedWeeklyVerse? = remembered

    override fun rememberWeekly(verse: RememberedWeeklyVerse) {
        remembered = verse
        writes++
    }
}
