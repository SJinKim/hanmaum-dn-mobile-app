package com.hanmaum.dn.mobile.features.profile.presentation

import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: MemberResponse,
        val isEditing: Boolean = false,
        val editPhone: String = profile.phoneNumber ?: "",
        val editImageUrl: String = profile.profileImageUrl ?: "",
        val isSaving: Boolean = false,
        val saveError: String? = null,
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
