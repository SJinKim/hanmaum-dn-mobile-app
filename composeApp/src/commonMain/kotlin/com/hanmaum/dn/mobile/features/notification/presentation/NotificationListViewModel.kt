package com.hanmaum.dn.mobile.features.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationType
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationListUiState(
    val items: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
) {
    val allRead: Boolean get() = items.all { it.isRead }
}

class NotificationListViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationListUiState())
    val uiState = _uiState.asStateFlow()

    private val _openAnnouncement = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openAnnouncement = _openAnnouncement.asSharedFlow()

    private var page = 0

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.items.isEmpty(), error = null) }
            repository.getNotifications(page = 0)
                .onSuccess { result ->
                    page = 0
                    _uiState.update { it.copy(isLoading = false, items = result.items, hasNext = result.hasNext) }
                    // Opening the screen means everything is now "seen" (badge -> 0).
                    repository.markAllSeen()
                }
                .onFailure { _ ->
                    _uiState.update { it.copy(isLoading = false, error = "알림을 불러오지 못했습니다") }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasNext || state.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.getNotifications(page = page + 1)
                .onSuccess { result ->
                    page += 1
                    _uiState.update { it.copy(isLoadingMore = false, items = it.items + result.items, hasNext = result.hasNext) }
                }
                .onFailure { _uiState.update { it.copy(isLoadingMore = false) } }
        }
    }

    fun onItemClick(notification: Notification) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.publicId == notification.publicId) it.copy(isRead = true) else it })
        }
        viewModelScope.launch { repository.markRead(notification.publicId) }
        val ref = notification.reference
        if (ref != null && ref.type == NotificationType.ANNOUNCEMENT) {
            _openAnnouncement.tryEmit(ref.publicId)
        }
    }

    fun onReadAll() {
        if (_uiState.value.allRead) return
        _uiState.update { state -> state.copy(items = state.items.map { it.copy(isRead = true) }) }
        viewModelScope.launch { repository.markAllRead() }
    }

    fun delete(notification: Notification) {
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.publicId == notification.publicId })
        }
        // Optimistic removal; resync from the server if the delete didn't land.
        viewModelScope.launch { repository.delete(notification.publicId).onFailure { load() } }
    }

    fun deleteAll() {
        if (_uiState.value.items.isEmpty()) return
        _uiState.update { it.copy(items = emptyList()) }
        viewModelScope.launch { repository.deleteAll().onFailure { load() } }
    }
}
