package com.hanmaum.dn.mobile.features.bulletin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.bulletin.domain.model.*
import com.hanmaum.dn.mobile.features.bulletin.domain.repository.BulletinRepository
import com.hanmaum.dn.mobile.features.bulletin.domain.repository.BulletinAccessDenied
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class BulletinUiState(
    val isLoading: Boolean = true,
    val content: BulletinRead? = null,
    val failed: Boolean = false,
    val selectedDate: LocalDate? = null,
    val selectedTab: Int = 0,
    val historyOpen: Boolean = false,
    val historyLoading: Boolean = false,
    val historyFailed: Boolean = false,
    val history: List<BulletinSummary> = emptyList(),
    val historyHasNext: Boolean = false,
)

class BulletinViewModel(private val repository: BulletinRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(BulletinUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var historyJob: Job? = null
    private var nextPage = 0

    init { refresh() }

    fun refresh() {
        loadJob?.cancel()
        val date = _uiState.value.selectedDate
        _uiState.update { it.copy(isLoading = true, failed = false) }
        loadJob = viewModelScope.launch {
            val result = if (date == null) repository.getCurrent() else repository.getByDate(date)
            result.fold(
                onSuccess = { value -> _uiState.update { it.copy(isLoading = false, content = value, failed = false) } },
                // Repository alone decides whether a cached copy is still safe to display.
                onFailure = { _uiState.update { it.copy(isLoading = false, content = null, failed = true) } },
            )
        }
    }

    fun selectTab(tab: Int) { _uiState.update { it.copy(selectedTab = tab.coerceIn(0, 1)) } }
    fun selectEdition(date: LocalDate?) {
        _uiState.update { it.copy(selectedDate = date, selectedTab = 0, content = null, historyOpen = false) }
        refresh()
    }
    fun openHistory() {
        historyJob?.cancel()
        nextPage = 0
        _uiState.update { it.copy(historyOpen = true, history = emptyList(), historyLoading = false, historyHasNext = false) }
        loadMoreHistory()
    }
    fun closeHistory() { _uiState.update { it.copy(historyOpen = false) } }
    fun loadMoreHistory() {
        if (_uiState.value.historyLoading) return
        if (nextPage > 0 && !_uiState.value.historyHasNext && !_uiState.value.historyFailed) return
        val page = nextPage
        _uiState.update { it.copy(historyLoading = true, historyFailed = false) }
        historyJob = viewModelScope.launch {
            repository.getHistory(page).fold(
                onSuccess = { result ->
                    nextPage = page + 1
                    _uiState.update { it.copy(
                        historyLoading = false,
                        history = (it.history + result.editions).distinctBy(BulletinSummary::publicId),
                        historyHasNext = result.hasNext,
                    ) }
                },
                onFailure = { cause ->
                    if (cause is BulletinAccessDenied) {
                        loadJob?.cancel()
                        _uiState.update { it.copy(content = null, failed = true, isLoading = false, history = emptyList()) }
                    }
                    _uiState.update { it.copy(historyLoading = false, historyFailed = true) }
                },
            )
        }
    }
}
