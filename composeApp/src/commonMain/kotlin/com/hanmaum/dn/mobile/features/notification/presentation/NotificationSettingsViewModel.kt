package com.hanmaum.dn.mobile.features.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val pushEnabled: Boolean = true,
    val isLoading: Boolean = false,
)

class NotificationSettingsViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getPushEnabled()
                .onSuccess { enabled -> _uiState.update { it.copy(isLoading = false, pushEnabled = enabled) } }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    fun onToggle(enabled: Boolean) {
        val previous = _uiState.value.pushEnabled
        _uiState.update { it.copy(pushEnabled = enabled) }
        viewModelScope.launch {
            repository.setPushEnabled(enabled)
                .onFailure { _uiState.update { it.copy(pushEnabled = previous) } }
        }
    }
}
