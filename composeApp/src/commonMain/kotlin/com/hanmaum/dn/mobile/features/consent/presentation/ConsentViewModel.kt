package com.hanmaum.dn.mobile.features.consent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentException
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentProblem
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentStatus
import com.hanmaum.dn.mobile.features.consent.domain.model.MemberConsents
import com.hanmaum.dn.mobile.features.consent.domain.model.PolicyConsentState
import com.hanmaum.dn.mobile.features.consent.domain.model.PrivacyPolicyLink
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentRepository
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentSessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConsentPhase { LOADING, REQUIRED, READY, ERROR }

data class ConsentUiState(
    val phase: ConsentPhase = ConsentPhase.LOADING,
    val summary: MemberConsents? = null,
    val selection: PolicyConsentState = PolicyConsentState(),
    val isBusy: Boolean = false,
    val isRefreshing: Boolean = false,
    val settingsProblem: ConsentProblem? = null,
    val showWithdrawConfirmation: Boolean = false,
    val signedOut: Boolean = false,
    /** The server could not be asked; the member keeps using the app, location stays off. */
    val unverified: Boolean = false,
)

/** One instance for the app session, shared by every protected destination. */
class ConsentViewModel(
    private val repository: ConsentRepository,
    private val session: ConsentSessionRepository,
    val policyLink: PrivacyPolicyLink,
    consentRequired: Flow<Unit> = emptyFlow(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState = _uiState.asStateFlow()
    private var checkedScope: String? = null
    private var job: Job? = null
    private var generation = 0
    private val activeDestination = MutableStateFlow(false)

    /**
     * Whether the location features may run: consent is verified, and the member is on a
     * destination of an active account. The shell starts and stops the geofence from this.
     */
    val locationAllowed: StateFlow<Boolean> = combine(_uiState, activeDestination) { state, active ->
        active && state.phase == ConsentPhase.READY && !state.unverified
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // A 403 CONSENT_REQUIRED from any call means the server no longer allows this member in:
        // re-ask it, which lands on the lock screen. The check is a no-op while signed out or mid-load.
        viewModelScope.launch { consentRequired.collect { if (hasCurrentSession()) refresh() } }
    }

    fun hasCurrentSession(): Boolean = checkedScope != null && checkedScope == session.scope()

    fun reset() {
        generation++
        job?.cancel()
        checkedScope = null
        _uiState.value = ConsentUiState()
    }

    fun checkSession(active: Boolean) {
        activeDestination.value = active
        val scope = session.scope()
        if (scope == null) {
            reset()
            _uiState.value = ConsentUiState(signedOut = true)
            return
        }
        if (scope != checkedScope) {
            reset()
            checkedScope = scope
            refresh()
        } else if (_uiState.value.unverified) refresh()
    }

    private fun isCurrent(ticket: Int): Boolean = ticket == generation && checkedScope != null &&
        checkedScope == session.scope()

    fun refresh() {
        if (_uiState.value.isBusy || _uiState.value.isRefreshing) return
        if (!hasCurrentSession()) {
            checkSession(activeDestination.value)
            return
        }
        val ticket = generation
        _uiState.update { it.copy(isRefreshing = true, settingsProblem = null) }
        job?.cancel()
        job = viewModelScope.launch {
            repository.myConsents().fold(
                onSuccess = { result ->
                    if (!isCurrent(ticket)) return@fold
                    if (result.allowsAccess) {
                        _uiState.value = ConsentUiState(phase = ConsentPhase.READY, summary = result)
                    } else {
                        _uiState.value = ConsentUiState(phase = ConsentPhase.REQUIRED, summary = result)
                        loadPolicy(ticket)
                    }
                },
                onFailure = {
                    if (!isCurrent(ticket)) return@fold
                    // An outage must not lock members out. Only a server answer that says consent is
                    // missing blocks the app; without an answer the member stays in and we re-check later.
                    _uiState.update { current ->
                        when (current.phase) {
                            ConsentPhase.READY -> current.copy(isRefreshing = false, settingsProblem = ConsentProblem.LOAD)
                            ConsentPhase.LOADING -> current.copy(
                                phase = ConsentPhase.READY, isRefreshing = false, unverified = true,
                                settingsProblem = ConsentProblem.LOAD,
                            )
                            else -> current.copy(phase = ConsentPhase.ERROR, isRefreshing = false)
                        }
                    }
                },
            )
        }
    }

    fun retryPolicy() {
        if (_uiState.value.isBusy || _uiState.value.selection.isLoading) return
        val ticket = generation
        _uiState.update { it.copy(selection = PolicyConsentState()) }
        job?.cancel()
        job = viewModelScope.launch { loadPolicy(ticket) }
    }

    private suspend fun loadPolicy(ticket: Int, outdated: Boolean = false) {
        val result = repository.policies()
        if (!isCurrent(ticket)) return
        _uiState.update { current ->
            current.copy(selection = result.fold(
                onSuccess = { policies ->
                    val policy = policies.singleOrNull { it.purpose == ConsentPurpose.APP_PRIVACY }
                    PolicyConsentState(
                        isLoading = false, policy = policy,
                        problem = when {
                            policy == null -> ConsentProblem.LOAD
                            !policyLink.isAvailable -> ConsentProblem.POLICY_UNAVAILABLE
                            outdated -> ConsentProblem.VERSION_OUTDATED
                            else -> null
                        },
                    )
                },
                onFailure = { PolicyConsentState(isLoading = false, problem = ConsentProblem.LOAD) },
            ), isBusy = false, isRefreshing = false)
        }
    }

    fun setAccepted(accepted: Boolean) {
        if (_uiState.value.isBusy || !policyLink.isAvailable) return
        _uiState.update {
            it.copy(selection = it.selection.copy(
                acceptedVersion = it.selection.policy?.version?.takeIf { _ -> accepted },
            ))
        }
    }

    fun grant() {
        val selection = _uiState.value.selection
        if (_uiState.value.isBusy || _uiState.value.isRefreshing || !selection.canSubmit || !policyLink.isAvailable) return
        val version = selection.policy?.version ?: return
        val ticket = generation
        _uiState.update { it.copy(isBusy = true) }
        job?.cancel()
        job = viewModelScope.launch {
            repository.grant(ConsentPurpose.APP_PRIVACY, version).fold(
                onSuccess = {
                    if (!isCurrent(ticket)) return@fold
                    // Granting one purpose is not proof that every required purpose is satisfied.
                    _uiState.update { it.copy(isBusy = false) }
                    refresh()
                },
                onFailure = { error ->
                    if (!isCurrent(ticket)) return@fold
                    if ((error as? ConsentException)?.problem == ConsentProblem.VERSION_OUTDATED) {
                        _uiState.update { it.copy(selection = PolicyConsentState()) }
                        loadPolicy(ticket, outdated = true)
                    } else {
                        _uiState.update { it.copy(isBusy = false,
                            selection = it.selection.copy(problem = ConsentProblem.SAVE)) }
                    }
                },
            )
        }
    }

    fun requestWithdrawal() {
        if (!_uiState.value.isBusy && !_uiState.value.isRefreshing &&
            _uiState.value.phase == ConsentPhase.READY &&
            _uiState.value.summary?.privacy?.state == ConsentStatus.GRANTED) {
            _uiState.update { it.copy(showWithdrawConfirmation = true) }
        }
    }

    fun dismissWithdrawal() {
        if (!_uiState.value.isBusy) _uiState.update { it.copy(showWithdrawConfirmation = false) }
    }

    fun confirmWithdrawal() {
        if (!_uiState.value.showWithdrawConfirmation || _uiState.value.isBusy) return
        val ticket = generation
        _uiState.update { it.copy(isBusy = true, settingsProblem = null) }
        job?.cancel()
        job = viewModelScope.launch {
            repository.withdraw(ConsentPurpose.APP_PRIVACY).fold(
                onSuccess = {
                    if (!isCurrent(ticket)) return@fold
                    // The member stays signed in but is locked until they agree again; location stops
                    // with the lock. This does not delete the account.
                    _uiState.value = ConsentUiState(phase = ConsentPhase.LOADING)
                    refresh()
                },
                onFailure = {
                    if (!isCurrent(ticket)) return@fold
                    _uiState.update { it.copy(isBusy = false, showWithdrawConfirmation = false,
                        settingsProblem = ConsentProblem.WITHDRAW) }
                },
            )
        }
    }

    fun logout() {
        if (_uiState.value.isBusy) return
        generation++
        val ticket = generation
        _uiState.update { it.copy(isBusy = true) }
        job?.cancel()
        job = viewModelScope.launch {
            session.logout()
            if (ticket == generation) _uiState.value = ConsentUiState(signedOut = true)
        }
    }
}
