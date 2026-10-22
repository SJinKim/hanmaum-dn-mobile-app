package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LocaleRepositoryImplTest {
    @Test fun firstRunDoesNotPersistAnImplicitEnglishChoice() {
        val settings = MapSettings()
        val repository = LocaleRepositoryImpl(settings)
        assertEquals(AppLocale.EN, repository.getLocale())
        assertNull(repository.getSelectedLocale())
        assertFalse(settings.hasKey("app_locale"))
    }

    @Test fun supportedExplicitChoicesSurviveRepositoryRecreation() {
        AppLocale.entries.forEach { locale ->
            val settings = MapSettings()
            LocaleRepositoryImpl(settings).setLocale(locale)
            assertEquals(locale, LocaleRepositoryImpl(settings).getSelectedLocale())
        }
    }

    @Test fun existingExplicitEnglishAndKoreanChoicesNeedNoNewConfirmation() {
        listOf("en" to AppLocale.EN, "ko" to AppLocale.KO).forEach { (code, locale) ->
            assertEquals(locale, LocaleRepositoryImpl(MapSettings("app_locale" to code)).getSelectedLocale())
        }
    }

    @Test fun germanAndUnknownLegacyValuesRequireAnExplicitChoice() {
        listOf("de", "xx", "").forEach { code ->
            val repository = LocaleRepositoryImpl(MapSettings("app_locale" to code))
            assertNull(repository.getSelectedLocale())
            assertEquals(AppLocale.EN, repository.getLocale())
        }
    }
}
