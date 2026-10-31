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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState = _uiState.asStateFlow()
    private var checkedScope: String? = null
    private var job: Job? = null
    private var locationJob: Job? = null
    private var generation = 0
    private var activeDestination = false
    private var monitoringStarted = false

    fun hasCurrentSession(): Boolean = checkedScope != null && checkedScope == session.scope()

    fun reset() {
        generation++
        job?.cancel()
        locationJob?.cancel()
        viewModelScope.launch { session.stopLocation() }
        checkedScope = null
        monitoringStarted = false
        _uiState.value = ConsentUiState()
    }

    fun checkSession(active: Boolean) {
        activeDestination = active
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
        else if (_uiState.value.phase == ConsentPhase.READY) startLocationIfAllowed()
    }

    private fun isCurrent(ticket: Int): Boolean = ticket == generation && checkedScope != null &&
        checkedScope == session.scope()

    fun refresh() {
        if (_uiState.value.isBusy || _uiState.value.isRefreshing) return
        if (!hasCurrentSession()) {
            checkSession(activeDestination)
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
                        startLocationIfAllowed()
                    } else {
                        monitoringStarted = false
                        locationJob?.cancelAndJoin()
                        session.stopLocation()
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

    private fun startLocationIfAllowed() {
        if (!activeDestination || monitoringStarted || _uiState.value.phase != ConsentPhase.READY ||
            _uiState.value.unverified) return
        monitoringStarted = true
        val ticket = generation
        locationJob = viewModelScope.launch {
            if (isCurrent(ticket) && _uiState.value.phase == ConsentPhase.READY && !_uiState.value.unverified) {
                session.startLocation()
            }
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
                    // The member stays signed in but is locked until they agree again; location stops now.
                    // This does not delete the account.
                    monitoringStarted = false
                    locationJob?.cancelAndJoin()
                    session.stopLocation()
                    if (!isCurrent(ticket)) return@fold
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
        job?.cancel()
        val ticket = generation
        _uiState.update { it.copy(isBusy = true) }
        job?.cancel()
        job = viewModelScope.launch {
            locationJob?.cancelAndJoin()
            session.logout()
            if (ticket == generation) _uiState.value = ConsentUiState(signedOut = true)
        }
    }
}
