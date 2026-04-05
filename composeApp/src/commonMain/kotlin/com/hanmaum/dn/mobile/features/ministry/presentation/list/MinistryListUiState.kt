package com.hanmaum.dn.mobile.features.ministry.presentation.list

import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry

sealed class MinistryListUiState {
    object Loading : MinistryListUiState()
    data class Success(val ministries: List<Ministry>) : MinistryListUiState()
    data class Error(val message: String) : MinistryListUiState()
}
