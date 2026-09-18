package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MinistryDetailViewModel(
    private val publicId: String,
    private val repository: MinistryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MinistryDetailUiState>(MinistryDetailUiState.Loading)
    val uiState: StateFlow<MinistryDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * One request. This used to fetch the member's own registration alongside the detail,
     * which meant a 404 on every open against an endpoint the server never had — swallowed
     * as "not registered", so it looked like an answer (#248).
     */
    fun load() {
        viewModelScope.launch {
            _uiState.value = MinistryDetailUiState.Loading
            repository.getMinistryDetail(publicId).fold(
                onSuccess = { _uiState.value = MinistryDetailUiState.Success(detail = it) },
                onFailure = {
                    _uiState.value = MinistryDetailUiState.Error(it.message ?: "부서 정보 로딩 실패")
                },
            )
        }
    }
}
