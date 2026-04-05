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

    init {
        loadMinistries()
    }

    fun loadMinistries() {
        viewModelScope.launch {
            _uiState.value = MinistryListUiState.Loading
            repository.getMinistries(activeOnly = true).fold(
                onSuccess = { _uiState.value = MinistryListUiState.Success(it) },
                onFailure = { _uiState.value = MinistryListUiState.Error(it.message ?: "부서 목록 로딩 실패") },
            )
        }
    }
}
