package com.hanmaum.dn.mobile.features.consent

import com.hanmaum.dn.mobile.features.consent.domain.model.*
import com.hanmaum.dn.mobile.features.consent.presentation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeConsentRepository
    private lateinit var session: FakeConsentSession
    private lateinit var vm: ConsentViewModel

    @BeforeTest fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeConsentRepository()
        session = FakeConsentSession()
        vm = ConsentViewModel(repository, session, PrivacyPolicyLink("https://church.example/privacy"))
    }
    @AfterTest fun cleanup() = Dispatchers.resetMain()

    private fun missing() {
        repository.summaryResult = Result.success(MemberConsents(true, listOf(testConsent(ConsentStatus.MISSING))))
        vm.checkSession(active = true)
    }

    @Test fun missingConsentNeverStartsLocationOrGrantsWithoutSelection() = runTest {
        missing(); advanceUntilIdle(); vm.grant(); advanceUntilIdle()
        assertEquals(ConsentPhase.REQUIRED, vm.uiState.value.phase)
        assertFalse(vm.uiState.value.selection.accepted)
        assertEquals(0, repository.grantCalls)
        assertEquals(0, session.starts)
    }

    @Test fun agreeingRechecksTheServerBeforeAllowingTheApp() = runTest {
        missing(); advanceUntilIdle()
        repository.afterGrant = { repository.summaryResult = Result.success(MemberConsents(false, listOf(testConsent()))) }
        vm.setAccepted(true); vm.grant(); advanceUntilIdle()
        assertEquals("policy-1", repository.grantedVersion)
        assertEquals(2, repository.summaryCalls)
        assertEquals(ConsentPhase.READY, vm.uiState.value.phase)
        assertEquals(1, session.starts)
    }

    @Test fun aServerVersionConflictRefetchesAndClearsSelection() = runTest {
        missing(); advanceUntilIdle(); vm.setAccepted(true)
        repository.grantResult = Result.failure(ConsentException(ConsentProblem.VERSION_OUTDATED))
        repository.policyResult = Result.success(listOf(TestPrivacyPolicy.copy(version = "policy-2")))
        vm.grant(); advanceUntilIdle()
        assertEquals("policy-2", vm.uiState.value.selection.policy?.version)
        assertFalse(vm.uiState.value.selection.accepted)
        assertEquals(ConsentProblem.VERSION_OUTDATED, vm.uiState.value.selection.problem)
        assertEquals(ConsentPhase.REQUIRED, vm.uiState.value.phase)
    }

    @Test fun aServerOutageDoesNotLockMembersOutAndKeepsLocationOff() = runTest {
        repository.summaryResult = Result.failure(Exception("offline"))
        vm.checkSession(true); advanceUntilIdle()
        assertEquals(ConsentPhase.READY, vm.uiState.value.phase)
        assertTrue(vm.uiState.value.unverified)
        assertEquals(0, session.starts)
    }

    @Test fun anUnverifiedSessionIsCheckedAgainOnTheNextDestination() = runTest {
        repository.summaryResult = Result.failure(Exception("offline"))
        vm.checkSession(true); advanceUntilIdle()
        repository.summaryResult = Result.success(MemberConsents(true, listOf(testConsent(ConsentStatus.MISSING))))
        vm.checkSession(true); advanceUntilIdle()
        assertEquals(ConsentPhase.REQUIRED, vm.uiState.value.phase)
        assertFalse(vm.uiState.value.unverified)
    }

    @Test fun recoveryAfterAnOutageStartsLocation() = runTest {
        repository.summaryResult = Result.failure(Exception("offline"))
        vm.checkSession(true); advanceUntilIdle()
        repository.summaryResult = Result.success(MemberConsents(false, listOf(testConsent())))
        vm.checkSession(true); advanceUntilIdle()
        assertFalse(vm.uiState.value.unverified)
        assertEquals(1, session.starts)
    }

    @Test fun grantFailureKeepsSelectionButDoesNotClaimASavedGrant() = runTest {
        missing(); advanceUntilIdle(); vm.setAccepted(true)
        repository.grantResult = Result.failure(Exception("offline"))
        vm.grant(); advanceUntilIdle()
        assertEquals(ConsentProblem.SAVE, vm.uiState.value.selection.problem)
        assertTrue(vm.uiState.value.selection.accepted)
        assertEquals(ConsentPhase.REQUIRED, vm.uiState.value.phase)
    }

    @Test fun withdrawalNeedsConfirmationAndLocksTheSignedInMemberAfterServerSuccess() = runTest {
        vm.checkSession(true); advanceUntilIdle()
        vm.confirmWithdrawal(); advanceUntilIdle()
        assertEquals(0, repository.withdrawCalls)
        vm.requestWithdrawal()
        assertTrue(vm.uiState.value.showWithdrawConfirmation)
        val stopsBefore = session.stops
        repository.afterWithdraw = {
            repository.summaryResult = Result.success(MemberConsents(true, listOf(testConsent(ConsentStatus.MISSING))))
        }
        vm.confirmWithdrawal(); advanceUntilIdle()
        assertEquals(1, repository.withdrawCalls)
        assertEquals(0, session.logoutCalls)
        assertFalse(vm.uiState.value.signedOut)
        assertEquals(ConsentPhase.REQUIRED, vm.uiState.value.phase)
        assertTrue(session.stops > stopsBefore)
    }

    @Test fun failedWithdrawalKeepsTheSessionAndShowsAnError() = runTest {
        vm.checkSession(true); advanceUntilIdle()
        repository.withdrawResult = Result.failure(Exception("offline"))
        vm.requestWithdrawal(); vm.confirmWithdrawal(); advanceUntilIdle()
        assertEquals(0, session.logoutCalls)
        assertEquals(ConsentProblem.WITHDRAW, vm.uiState.value.settingsProblem)
        assertEquals(ConsentPhase.READY, vm.uiState.value.phase)
    }

    @Test fun anotherAccountCannotReuseAConfirmedSession() = runTest {
        vm.checkSession(true); advanceUntilIdle()
        session.identity = "member-2"
        assertFalse(vm.hasCurrentSession())
        repository.summaryResult = Result.success(MemberConsents(true, listOf(testConsent(ConsentStatus.MISSING))))
        vm.checkSession(true); advanceUntilIdle()
        assertEquals(ConsentPhase.REQUIRED, vm.uiState.value.phase)
    }

    @Test fun aResponseForAnOldSessionIsDiscarded() = runTest {
        val waiting = CompletableDeferred<Unit>()
        repository.awaitSummary = { waiting.await() }
        vm.checkSession(true); runCurrent()
        session.identity = "member-2"
        waiting.complete(Unit); advanceUntilIdle()
        assertFalse(vm.hasCurrentSession())
        assertNotEquals(ConsentPhase.READY, vm.uiState.value.phase)
    }

    @Test fun noPublishedPolicyMeansNoConsentSubmission() = runTest {
        vm = ConsentViewModel(repository, session, PrivacyPolicyLink(null))
        missing(); advanceUntilIdle(); vm.setAccepted(true); vm.grant(); advanceUntilIdle()
        assertEquals(ConsentProblem.POLICY_UNAVAILABLE, vm.uiState.value.selection.problem)
        assertEquals(0, repository.grantCalls)
    }

    @Test fun pendingMembersDoNotStartLocationAfterConsent() = runTest {
        vm.checkSession(false); advanceUntilIdle()
        assertEquals(ConsentPhase.READY, vm.uiState.value.phase)
        assertEquals(0, session.starts)
        vm.checkSession(true); advanceUntilIdle()
        assertEquals(1, session.starts)
    }

    @Test fun aRestoredRouteWithoutASessionReturnsToLoginWithoutFetchingPrivateData() = runTest {
        session.identity = null
        vm.checkSession(true); advanceUntilIdle()
        assertTrue(vm.uiState.value.signedOut)
        assertEquals(0, repository.summaryCalls)
        assertEquals(0, session.starts)
    }
}
