package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail

sealed class MinistryDetailUiState {
    object Loading : MinistryDetailUiState()

    /**
     * Only the 사역 itself. There is nothing about the member's own standing to hold: 사역
     * self-registration does not exist on the server yet (hanmaum-dn-server#170, #248).
     */
    data class Success(val detail: MinistryDetail) : MinistryDetailUiState()

    data class Error(val message: String) : MinistryDetailUiState()
}
