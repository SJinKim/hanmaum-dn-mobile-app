package com.hanmaum.dn.mobile.core.presentation

import com.hanmaum.dn.mobile.core.data.repository.LocaleRepositoryImpl
import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.hanmaum.dn.mobile.core.security.FakeSecureStore
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageViewModelTest {
    @Test fun firstRunRequiresSelectionBeforeAuthentication() {
        assertTrue(LanguageViewModel(LocaleRepositoryImpl(MapSettings())).uiState.value.needsSelection)
    }

    @Test fun eitherLanguageConfirmsImmediatelyAndSurvivesRestart() {
        AppLocale.entries.forEach { locale ->
            val settings = MapSettings()
            val vm = LanguageViewModel(LocaleRepositoryImpl(settings))
            vm.select(locale)
            assertFalse(vm.uiState.value.needsSelection)
            assertEquals(locale, vm.uiState.value.locale)
            val restarted = LanguageViewModel(LocaleRepositoryImpl(settings))
            assertFalse(restarted.uiState.value.needsSelection)
            assertEquals(locale, restarted.uiState.value.locale)
        }
    }

    @Test fun settingsChangeUpdatesStateWithoutClearingTheSession() {
        val settings = MapSettings("app_locale" to "en")
        val tokens = TokenStorageImpl(FakeSecureStore(), settings)
        tokens.saveAccessToken("test-access")
        tokens.saveRefreshToken("test-refresh")
        val vm = LanguageViewModel(LocaleRepositoryImpl(settings))
        vm.select(AppLocale.KO)
        assertEquals(AppLocale.KO, vm.uiState.value.locale)
        assertEquals("test-access", tokens.getAccessToken())
        assertEquals("test-refresh", tokens.getRefreshToken())
        vm.select(AppLocale.EN)
        assertEquals(AppLocale.EN, vm.uiState.value.locale)
    }

    @Test fun logoutKeepsTheExplicitLanguageForTheNextLogin() {
        val settings = MapSettings()
        val vm = LanguageViewModel(LocaleRepositoryImpl(settings))
        vm.select(AppLocale.KO)
        TokenStorageImpl(FakeSecureStore(), settings).clear()
        val restarted = LanguageViewModel(LocaleRepositoryImpl(settings))
        assertFalse(restarted.uiState.value.needsSelection)
        assertEquals(AppLocale.KO, restarted.uiState.value.locale)
    }

    @Test fun unsupportedSavedLanguageShowsThePicker() {
        val vm = LanguageViewModel(LocaleRepositoryImpl(MapSettings("app_locale" to "de")))
        assertTrue(vm.uiState.value.needsSelection)
        vm.select(AppLocale.EN)
        assertFalse(vm.uiState.value.needsSelection)
    }
}
