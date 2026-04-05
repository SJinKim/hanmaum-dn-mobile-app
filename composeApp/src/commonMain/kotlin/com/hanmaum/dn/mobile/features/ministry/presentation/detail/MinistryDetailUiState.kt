package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.model.MyRegistration
import com.hanmaum.dn.mobile.features.ministry.domain.model.RegistrationStatus

sealed class MinistryDetailUiState {
    object Loading : MinistryDetailUiState()
    data class Success(
        val detail: MinistryDetail,
        val registration: MyRegistration?,
        val showSheet: Boolean = false,
        val noteInput: String = "",
        val isRegistering: Boolean = false,
        val registerError: String? = null,
    ) : MinistryDetailUiState() {
        val registrationStatus: RegistrationStatus
            get() = registration?.status ?: RegistrationStatus.NONE
    }
    data class Error(val message: String) : MinistryDetailUiState()
}
