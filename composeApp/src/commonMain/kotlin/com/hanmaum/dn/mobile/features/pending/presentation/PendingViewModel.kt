package com.hanmaum.dn.mobile.features.pending.presentation

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
            _uiState.update { it.copy(isLoading = true, message = "계정 상태 확인중입니다.") }

            val result = memberRepository.getMyProfile()

            result.onSuccess { member ->
                if (member.status == MemberStatus.ACTIVE) {
                    _uiState.update { it.copy(isLoading = false, navigateTo = NavRoute.Home) }
                } else {
                    _uiState.update { it.copy(isLoading = false, message = "아직 준비중입니다. 기다려주세요.") }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, message = "인증을 실패하였습니다. 로그인 다시 시도해주세요.") }
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