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

    fun load() {
        viewModelScope.launch {
            _uiState.value = MinistryDetailUiState.Loading
            repository.getMinistryDetail(publicId).fold(
                onSuccess = { _uiState.value = MinistryDetailUiState.Success(it) },
                onFailure = { _uiState.value = MinistryDetailUiState.Error(it.message ?: "사역 정보 로딩 실패") },
            )
        }
    }
}
