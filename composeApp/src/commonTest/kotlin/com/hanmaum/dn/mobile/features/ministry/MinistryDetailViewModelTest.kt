package com.hanmaum.dn.mobile.features.ministry

import com.hanmaum.dn.mobile.features.ministry.domain.model.MyRegistration
import com.hanmaum.dn.mobile.features.ministry.domain.model.RegistrationStatus
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailUiState
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The detail screen and the self-registration behind it. #117 notes that the
 * registration flow — RegistrationSheet, RegistrationStatus, register() — had
 * no coverage at all, which is the more serious half of that gap.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MinistryDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeMinistryRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeMinistryRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = MinistryDetailViewModel("m1", repo)
    private fun success(vm: MinistryDetailViewModel) =
        assertIs<MinistryDetailUiState.Success>(vm.uiState.value)

    // ── loading ──────────────────────────────────────────────────────────

    @Test
    fun detailAndRegistrationArriveTogether() = runTest {
        repo.registrationResult = Result.success(
            MyRegistration("r1", RegistrationStatus.APPROVED, note = "기타 연주"),
        )
        val vm = vm()
        advanceUntilIdle()

        val s = success(vm)
        assertEquals("난민 사역", s.detail.name)
        assertEquals(RegistrationStatus.APPROVED, s.registrationStatus)
    }

    @Test
    fun noRegistrationReadsAsNone() = runTest {
        repo.registrationResult = Result.success(null)
        val vm = vm()
        advanceUntilIdle()

        assertEquals(RegistrationStatus.NONE, success(vm).registrationStatus)
    }

    @Test
    fun aFailedRegistrationLookupDoesNotCostTheDetail() = runTest {
        // The 사역 itself is readable without knowing whether I applied to it.
        repo.registrationResult = Result.failure(IllegalStateException("offline"))
        val vm = vm()
        advanceUntilIdle()

        val s = success(vm)
        assertEquals("난민 사역", s.detail.name)
        assertEquals(RegistrationStatus.NONE, s.registrationStatus)
    }

    @Test
    fun aFailedDetailIsAnError() = runTest {
        repo.detailResult = Result.failure(IllegalStateException("gone"))
        val vm = vm()
        advanceUntilIdle()

        assertEquals("gone", assertIs<MinistryDetailUiState.Error>(vm.uiState.value).message)
    }

    // ── the sheet ────────────────────────────────────────────────────────

    @Test
    fun openingTheSheetClearsWhatALastAttemptLeftBehind() = runTest {
        repo.registerResult = Result.failure(IllegalStateException("나중에 다시"))
        val vm = vm()
        advanceUntilIdle()
        vm.openSheet()
        vm.updateNote("기타 연주")
        vm.register()
        advanceUntilIdle()
        assertEquals("나중에 다시", success(vm).registerError)

        vm.openSheet()

        val s = success(vm)
        assertTrue(s.showSheet)
        assertEquals("", s.noteInput, "a stale note must not reappear")
        assertNull(s.registerError, "nor a stale error")
    }

    @Test
    fun closingTheSheetLeavesTheRegistrationAlone() = runTest {
        repo.registrationResult = Result.success(MyRegistration("r1", RegistrationStatus.PENDING, null))
        val vm = vm()
        advanceUntilIdle()
        vm.openSheet()
        vm.closeSheet()

        val s = success(vm)
        assertFalse(s.showSheet)
        assertEquals(RegistrationStatus.PENDING, s.registrationStatus)
    }

    // ── registering ──────────────────────────────────────────────────────

    @Test
    fun registeringSendsTheNoteAndClosesTheSheet() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.openSheet()
        vm.updateNote("기타 연주")
        vm.register()
        advanceUntilIdle()

        assertEquals(1, repo.registerCalls)
        assertEquals("m1", repo.lastRegisteredMinistry)
        assertEquals("기타 연주", repo.lastRegisteredNote)
        val s = success(vm)
        assertFalse(s.showSheet)
        assertFalse(s.isRegistering)
        assertEquals(RegistrationStatus.PENDING, s.registrationStatus)
    }

    @Test
    fun aBlankNoteIsSentAsNoNoteAtAll() = runTest {
        // Otherwise the backend stores an empty string where it means "none".
        val vm = vm()
        advanceUntilIdle()
        vm.openSheet()
        vm.updateNote("   ")
        vm.register()
        advanceUntilIdle()

        assertNull(repo.lastRegisteredNote)
    }

    @Test
    fun aFailedRegistrationKeepsTheSheetOpenWithItsMessage() = runTest {
        // Closing it would throw away what the member typed.
        repo.registerResult = Result.failure(IllegalStateException("이미 신청했습니다"))
        val vm = vm()
        advanceUntilIdle()
        vm.openSheet()
        vm.updateNote("기타 연주")
        vm.register()
        advanceUntilIdle()

        val s = success(vm)
        assertTrue(s.showSheet)
        assertEquals("기타 연주", s.noteInput)
        assertEquals("이미 신청했습니다", s.registerError)
        assertFalse(s.isRegistering)
    }

    @Test
    fun registeringBeforeTheDetailLoadedDoesNothing() = runTest {
        val vm = vm() // still Loading
        vm.register()
        advanceUntilIdle()

        assertEquals(0, repo.registerCalls)
    }
}
