package com.hanmaum.dn.mobile.core.presentation

import androidx.lifecycle.ViewModel
import com.hanmaum.dn.mobile.core.domain.repository.LocaleRepository
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LanguageUiState(val selectedLocale: AppLocale?) {
    val locale: AppLocale get() = selectedLocale ?: AppLocale.EN
    val needsSelection: Boolean get() = selectedLocale == null
}

/** One app-level owner: changing language never recreates the navigation host or session. */
class LanguageViewModel(private val repository: LocaleRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LanguageUiState(repository.getSelectedLocale()))
    val uiState = _uiState.asStateFlow()

    fun select(locale: AppLocale) {
        repository.setLocale(locale)
        _uiState.value = LanguageUiState(locale)
    }
}
