package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail

sealed class MinistryDetailUiState {
    object Loading : MinistryDetailUiState()
    data class Success(val detail: MinistryDetail) : MinistryDetailUiState()
    data class Error(val message: String) : MinistryDetailUiState()
}
