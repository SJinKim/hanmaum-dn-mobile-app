package com.hanmaum.dn.mobile.features.pending.presentation

import com.hanmaum.dn.mobile.core.i18n.UiMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.pending.data.model.PendingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PendingViewModel(
    private val memberRepository: MemberRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {
    private val _uiState = MutableStateFlow(PendingUiState())
    val uiState = _uiState.asStateFlow()

    fun onCheckStatusClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = UiMessage.CHECKING_STATUS) }

            val result = memberRepository.getMyProfile()

            result.onSuccess { member ->
                // Someone can be sitting on this screen at the moment the decision
                // is made. Without the REJECTED branch the button would keep
                // answering "please wait" for ever.
                when (member.status) {
                    MemberStatus.ACTIVE ->
                        _uiState.update { it.copy(isLoading = false, navigateTo = NavRoute.Home) }
                    MemberStatus.REJECTED ->
                        _uiState.update { it.copy(isLoading = false, navigateTo = NavRoute.Rejected) }
                    else ->
                        _uiState.update { it.copy(isLoading = false, message = UiMessage.STILL_PENDING) }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, message = UiMessage.STATUS_CHECK_FAILED) }
            }
        }
    }


    fun onLogoutClicked() {
        viewModelScope.launch {
            tokenStorage.clear()
            _uiState.update { it.copy(navigateTo = NavRoute.Login) }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateTo = null) }
    }

}