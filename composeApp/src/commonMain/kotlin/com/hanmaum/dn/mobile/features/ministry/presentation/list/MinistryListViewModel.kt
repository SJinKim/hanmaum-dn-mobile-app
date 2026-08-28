package com.hanmaum.dn.mobile.features.ministry.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MinistryListViewModel(
    private val repository: MinistryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MinistryListUiState>(MinistryListUiState.Loading)
    val uiState: StateFlow<MinistryListUiState> = _uiState.asStateFlow()

    /**
     * Loads (or reloads) the ministry list. Driven by the screen on every entry —
     * including returning to the tab — so server-side changes (e.g. a ministry
     * created from the web app) appear without needing to re-login. As a top-level
     * tab this ViewModel survives navigation, so an init-only load would go stale.
     */
    fun loadMinistries() {
        viewModelScope.launch {
            // Only show the spinner on the first load. On a silent refresh (returning
            // to the tab) keep the current list visible to avoid a flicker.
            val hadData = _uiState.value is MinistryListUiState.Success
            if (!hadData) _uiState.value = MinistryListUiState.Loading
            repository.getMinistries(activeOnly = true).fold(
                onSuccess = { _uiState.value = MinistryListUiState.Success(it) },
                onFailure = {
                    // Don't wipe already-shown data on a transient refresh failure.
                    if (!hadData) {
                        _uiState.value = MinistryListUiState.Error(it.message ?: "사역 목록 로딩 실패")
                    }
                },
            )
        }
    }
}
