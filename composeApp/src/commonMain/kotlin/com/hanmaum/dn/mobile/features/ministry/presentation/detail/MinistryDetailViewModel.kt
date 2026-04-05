package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MinistryDetailViewModel(
    private val publicId: String,
    private val repository: MinistryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MinistryDetailUiState>(MinistryDetailUiState.Loading)
    val uiState: StateFlow<MinistryDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MinistryDetailUiState.Loading
            val detailDeferred = async { repository.getMinistryDetail(publicId) }
            val regDeferred = async { repository.getMyRegistration(publicId) }

            val detailResult = detailDeferred.await()
            val regResult = regDeferred.await()

            if (detailResult.isFailure) {
                _uiState.value = MinistryDetailUiState.Error(
                    detailResult.exceptionOrNull()?.message ?: "부서 정보 로딩 실패"
                )
                return@launch
            }

            _uiState.value = MinistryDetailUiState.Success(
                detail = detailResult.getOrThrow(),
                registration = regResult.getOrNull(),
            )
        }
    }

    fun openSheet() {
        val current = _uiState.value as? MinistryDetailUiState.Success ?: return
        _uiState.value = current.copy(showSheet = true, noteInput = "", registerError = null)
    }

    fun closeSheet() {
        val current = _uiState.value as? MinistryDetailUiState.Success ?: return
        _uiState.value = current.copy(showSheet = false)
    }

    fun updateNote(value: String) {
        val current = _uiState.value as? MinistryDetailUiState.Success ?: return
        _uiState.value = current.copy(noteInput = value)
    }

    fun register() {
        val current = _uiState.value as? MinistryDetailUiState.Success ?: return
        _uiState.value = current.copy(isRegistering = true, registerError = null)
        viewModelScope.launch {
            repository.register(
                ministryPublicId = publicId,
                note = current.noteInput.ifBlank { null },
            ).fold(
                onSuccess = { reg ->
                    _uiState.value = current.copy(
                        registration = reg,
                        showSheet = false,
                        isRegistering = false,
                    )
                },
                onFailure = { err ->
                    _uiState.value = current.copy(
                        isRegistering = false,
                        registerError = err.message ?: "신청 실패",
                    )
                },
            )
        }
    }
}
