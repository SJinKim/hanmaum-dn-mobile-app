package com.hanmaum.dn.mobile.features.consent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.network.invalidateBearerCache
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentResult
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentState
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentStatus
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentRepository
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the consent flow sends the member next; the screen maps it to a route. */
enum class ConsentNavigation {
    /** Agreed. Splash routes by member status from here, as after any sign-in. */
    Continue,

    /** Withdrew in 설정. The account is locked until the member agrees again. */
    Withdrawn,

    /** Declined to agree and signed out instead. */
    LoggedOut,
}

data class ConsentUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** APP_PRIVACY as the server last reported it. */
    val status: ConsentStatus? = null,
    val accepted: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val showWithdrawDialog: Boolean = false,
    val isWithdrawing: Boolean = false,
    val withdrawFailed: Boolean = false,
    val navigateTo: ConsentNavigation? = null,
) {
    val isGranted: Boolean get() = status?.state == ConsentState.GRANTED

    val canGrant: Boolean get() = accepted && status != null && !isSaving
}

/**
 * The APP_PRIVACY consent, shared by the 설정 block and the consent screen (#290).
 *
 * Both read the same status and both change it, so the rules live once: agreement
 * is sent with the version the screen showed, never a constant, and a text that
 * changed in the meantime unticks the box rather than being agreed to silently.
 */
class ConsentViewModel(
    private val consentRepository: ConsentRepository,
    private val tokenStorage: TokenStorage,
    private val httpClient: HttpClient,
    private val notificationRepository: NotificationRepository,
    private val pushManager: PushManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            when (val result = consentRepository.getMyConsents()) {
                is ConsentResult.Success -> _uiState.update {
                    it.copy(isLoading = false, status = result.data.of(ConsentPurpose.APP_PRIVACY))
                }
                else -> _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onAcceptedChange(accepted: Boolean) {
        _uiState.update { it.copy(accepted = accepted) }
    }

    fun grant() {
        val current = _uiState.value
        val version = current.status?.currentVersion ?: return
        if (!current.canGrant) return
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            when (consentRepository.grant(ConsentPurpose.APP_PRIVACY, version)) {
                is ConsentResult.Success ->
                    _uiState.update { it.copy(isSaving = false, navigateTo = ConsentNavigation.Continue) }
                // The text changed while the screen was open. Show the new version
                // and ask again with the box unticked.
                ConsentResult.VersionOutdated -> {
                    _uiState.update { it.copy(isSaving = false, accepted = false) }
                    load()
                }
                // The box stays ticked: the member already agreed, only the call failed.
                else -> _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    fun onWithdrawRequested() {
        _uiState.update { it.copy(showWithdrawDialog = true, withdrawFailed = false) }
    }

    fun onWithdrawDismissed() {
        if (_uiState.value.isWithdrawing) return
        _uiState.update { it.copy(showWithdrawDialog = false, withdrawFailed = false) }
    }

    fun withdraw() {
        if (_uiState.value.isWithdrawing) return
        _uiState.update { it.copy(isWithdrawing = true, withdrawFailed = false) }
        viewModelScope.launch {
            when (consentRepository.withdraw(ConsentPurpose.APP_PRIVACY)) {
                is ConsentResult.Success -> _uiState.update {
                    it.copy(
                        isWithdrawing = false,
                        showWithdrawDialog = false,
                        accepted = false,
                        status = it.status?.copy(state = ConsentState.MISSING, grantedVersion = null, grantedAt = null),
                        navigateTo = ConsentNavigation.Withdrawn,
                    )
                }
                else -> _uiState.update { it.copy(isWithdrawing = false, withdrawFailed = true) }
            }
        }
    }

    /** Same steps as signing out in 프로필; push to this device stops first, while still authenticated. */
    fun logout() {
        viewModelScope.launch {
            pushManager.currentToken()?.let { notificationRepository.deleteDeviceToken(it) }
            tokenStorage.clear()
            httpClient.invalidateBearerCache()
            _uiState.update { it.copy(navigateTo = ConsentNavigation.LoggedOut) }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateTo = null) }
    }
}
