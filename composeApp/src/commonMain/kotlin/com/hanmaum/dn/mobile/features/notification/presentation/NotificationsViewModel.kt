package com.hanmaum.dn.mobile.features.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.notification.domain.model.AppNotification
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
    data class Success(val items: List<AppNotification>) : NotificationsUiState
}

class NotificationsViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            repository.getNotifications()
                .onSuccess { items ->
                    _uiState.value = NotificationsUiState.Success(items)
                    // opening the list is what clears the bell badge
                    repository.markSeen()
                }
                .onFailure { _uiState.value = NotificationsUiState.Error(it.message ?: "") }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead().onSuccess {
                val current = _uiState.value
                if (current is NotificationsUiState.Success) {
                    _uiState.value = current.copy(items = current.items.map { it.copy(isRead = true) })
                }
            }
        }
    }
}
