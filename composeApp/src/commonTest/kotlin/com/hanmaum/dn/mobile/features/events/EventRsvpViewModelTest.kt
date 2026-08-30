package com.hanmaum.dn.mobile.features.events

import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.RespondResult
import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EventRsvpViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeEventRsvpRepository
    private lateinit var prefs: FakeEventRsvpPreferences

    private val base = Instant.parse("2026-08-29T09:00:00Z")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeEventRsvpRepository()
        prefs = FakeEventRsvpPreferences()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun event(id: String, status: RsvpStatus? = null, deadlineInDays: Int = 3) = EventRsvp(
        publicId = id,
        title = "행사 $id",
        windowStart = base,
        windowEnd = base + deadlineInDays.days,
        announcementId = null,
        myStatus = status,
    )

    private fun vm() = EventRsvpViewModel(repo, prefs)

    @Test
    fun pendingHoldsUnansweredAndMaybeSortedByDeadline() = runTest(dispatcher) {
        repo.activeResult = Result.success(
            listOf(
                event("a", RsvpStatus.GOING, deadlineInDays = 1),
                event("b", null, deadlineInDays = 9),
                event("c", RsvpStatus.MAYBE, deadlineInDays = 4),
                event("d", RsvpStatus.NOT_GOING, deadlineInDays = 2),
            ),
        )
        val vm = vm()
        vm.refresh(); advanceUntilIdle()

        assertEquals(listOf("c", "b"), vm.uiState.value.pending.map { it.publicId })
        assertEquals(listOf("a", "d"), vm.uiState.value.answered.map { it.publicId })
        assertEquals(2, vm.uiState.value.pendingCount)
        assertEquals(1, vm.uiState.value.goingCount)
        assertEquals(1, vm.uiState.value.notGoingCount)
    }

    @Test
    fun theSheetIsOfferedOnlyForAGenuinelyUnansweredEvent() = runTest(dispatcher) {
        repo.activeResult = Result.success(listOf(event("maybe", RsvpStatus.MAYBE)))
        val vm = vm()
        vm.refresh(); advanceUntilIdle()

        assertFalse(vm.uiState.value.visible)
    }

    @Test
    fun theSheetStaysDownForAnEventAlreadyPutOff() = runTest(dispatcher) {
        prefs.handled += "a"
        repo.activeResult = Result.success(listOf(event("a")))
        val vm = vm()
        vm.refresh(); advanceUntilIdle()

        assertFalse(vm.uiState.value.visible)
        // Still pending though — the RSVP screen is the way back to it.
        assertEquals(listOf("a"), vm.uiState.value.pending.map { it.publicId })
    }

    @Test
    fun dismissingAnswersNothing() = runTest(dispatcher) {
        repo.activeResult = Result.success(listOf(event("a")))
        val vm = vm()
        vm.refresh(); advanceUntilIdle()
        assertTrue(vm.uiState.value.visible)

        vm.dismissSheet()

        assertFalse(vm.uiState.value.visible)
        assertEquals(emptyList(), repo.respondCalls)
        assertNull(vm.uiState.value.events.first().myStatus)
        assertTrue("a" in prefs.handled)
    }

    @Test
    fun respondingAppliesTheStatusImmediately() = runTest(dispatcher) {
        repo.activeResult = Result.success(listOf(event("a")))
        val vm = vm()
        vm.refresh(); advanceUntilIdle()

        vm.respond("a", RsvpStatus.NOT_GOING); advanceUntilIdle()

        assertEquals(RsvpStatus.NOT_GOING, vm.uiState.value.events.first().myStatus)
        assertNull(vm.uiState.value.respondingTo)
        assertEquals(listOf("a" to RsvpStatus.NOT_GOING), repo.respondCalls)
    }

    @Test
    fun aRefusedAnswerRevertsToThePreviousStatus() = runTest(dispatcher) {
        repo.activeResult = Result.success(listOf(event("a", RsvpStatus.MAYBE)))
        repo.respondResults["a"] = RespondResult.WindowClosed
        val vm = vm()
        vm.refresh(); advanceUntilIdle()

        vm.respond("a", RsvpStatus.GOING); advanceUntilIdle()

        assertEquals(RsvpStatus.MAYBE, vm.uiState.value.events.first().myStatus)
        assertNotNull(vm.uiState.value.rowErrors["a"])
    }

    @Test
    fun repeatingTheSameAnswerIsNotAnError() = runTest(dispatcher) {
        repo.activeResult = Result.success(listOf(event("a")))
        val vm = vm()
        vm.refresh(); advanceUntilIdle()

        vm.respond("a", RsvpStatus.GOING); advanceUntilIdle()
        vm.respond("a", RsvpStatus.GOING); advanceUntilIdle()

        assertTrue(vm.uiState.value.rowErrors.isEmpty())
        assertEquals(RsvpStatus.GOING, vm.uiState.value.events.first().myStatus)
        assertEquals(2, repo.respondCalls.size)
    }

    @Test
    fun answeringStopsTheSheetFromAskingAgain() = runTest(dispatcher) {
        repo.activeResult = Result.success(listOf(event("a")))
        val vm = vm()
        vm.refresh(); advanceUntilIdle()

        vm.respond("a", RsvpStatus.GOING); advanceUntilIdle()

        assertTrue("a" in prefs.handled)
    }

    @Test
    fun anEmptyListOffersNoSheetAndNoSections() = runTest(dispatcher) {
        repo.activeResult = Result.success(emptyList())
        val vm = vm()
        vm.refresh(); advanceUntilIdle()

        assertFalse(vm.uiState.value.visible)
        assertEquals(emptyList(), vm.uiState.value.pending)
        assertEquals(emptyList(), vm.uiState.value.answered)
    }

    @Test
    fun aLoadFailureSurfacesAnErrorWithoutBlockingHome() = runTest(dispatcher) {
        repo.activeResult = Result.failure(RuntimeException("network down"))
        val vm = vm()
        vm.refresh(); advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.visible)
        assertFalse(vm.uiState.value.isLoading)
    }
}
