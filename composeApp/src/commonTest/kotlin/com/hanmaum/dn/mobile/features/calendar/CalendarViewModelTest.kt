package com.hanmaum.dn.mobile.features.calendar

import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent
import com.hanmaum.dn.mobile.features.calendar.domain.repository.CalendarRepository
import com.hanmaum.dn.mobile.features.calendar.presentation.CalendarViewModel
import com.hanmaum.dn.mobile.features.calendar.presentation.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest  fun tearDown() { Dispatchers.resetMain() }

    private fun fakeEvent(day: Int, month: Int = 5) = CalendarEvent(
        id = "evt-$month-$day", title = "예배 $day", description = null,
        location = "본당",
        startDate = "2026-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
        endDate   = "2026-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
        isAllDay  = true,
    )

    /** Counts month fetches so a test can prove a request happened without asking for one. */
    private class CountingRepo(
        val events: List<CalendarEvent> = emptyList(),
        val yearEvents: List<CalendarEvent> = emptyList(),
        val monthFails: Boolean = false,
        val yearFails: Boolean = false,
    ) : CalendarRepository {
        var monthFetchCount = 0
        var yearFetchCount = 0

        override suspend fun getEvents(year: Int, month: Int): Result<List<CalendarEvent>> {
            monthFetchCount++
            return if (monthFails) Result.failure(Exception("boom")) else Result.success(events)
        }

        override suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>> {
            yearFetchCount++
            return if (yearFails) Result.failure(Exception("boom")) else Result.success(yearEvents)
        }
    }

    // The bug this feature was filed for: the v2 screen dropped its
    // LaunchedEffect and nothing asked for the month any more. Constructing the
    // view model is what entering the screen does, so the load has to happen
    // here — the test must never call refresh() to make it pass.
    @Test
    fun `entering the screen loads the month without anyone calling refresh`() = runTest {
        val repo = CountingRepo(events = listOf(fakeEvent(15)))
        val vm = CalendarViewModel(repo)

        assertTrue(vm.uiState.value.isLoading, "the first frame must be a load, not an empty calendar")

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repo.monthFetchCount)
        assertEquals(1, vm.uiState.value.events.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `a resume after the initial load fetches the month again`() = runTest {
        val repo = CountingRepo(events = listOf(fakeEvent(15)))
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, repo.monthFetchCount)
    }

    @Test
    fun `a resume that lands while the first load is still running does not fetch twice`() = runTest {
        val repo = CountingRepo(events = listOf(fakeEvent(15)))
        val vm = CalendarViewModel(repo)

        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repo.monthFetchCount)
    }

    // Fails if both the job cancellation and the write guard are taken out of
    // loadCurrentMonth; either one alone still passes it. What it pins down is
    // the outcome the issue asks for: the month the user landed on wins, and
    // the abandoned one cannot report its own result against it.
    @Test
    fun `a slow response for the previous month cannot overwrite the newer one`() = runTest {
        val repo = object : CalendarRepository {
            var firstMonthAsked: Int? = null

            // Mirrors CalendarRepositoryImpl, runCatching included, so an
            // abandoned call comes back as a failure rather than an exception.
            override suspend fun getEvents(year: Int, month: Int): Result<List<CalendarEvent>> = runCatching {
                if (firstMonthAsked == null) firstMonthAsked = month
                if (month == firstMonthAsked) {
                    delay(1_000)   // the month the screen opened on answers last
                    listOf(fakeEvent(1, month))
                } else {
                    delay(10)      // the month the user navigated to answers first
                    listOf(fakeEvent(2, month))
                }
            }

            override suspend fun getYearEvents(year: Int) = Result.success(emptyList<CalendarEvent>())
        }

        val vm = CalendarViewModel(repo)
        val slowMonth = vm.uiState.value.month
        dispatcher.scheduler.runCurrent()   // the first month is now in flight
        vm.nextMonth()
        dispatcher.scheduler.advanceUntilIdle()

        val shown = vm.uiState.value
        assertTrue(shown.month != slowMonth)
        assertEquals(fakeEvent(2, shown.month).id, shown.events.single().id)
        assertFalse(shown.monthLoadFailed, "the abandoned month must not fail the month now on screen")
        assertFalse(shown.isLoading)
    }

    @Test
    fun `a failed month load reports the failure instead of an empty month`() = runTest {
        val repo = CountingRepo(monthFails = true)
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.monthLoadFailed)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.events.isEmpty())
    }

    @Test
    fun `a failing refresh keeps the events that are already on screen`() = runTest {
        var fail = false
        val repo = object : CalendarRepository {
            override suspend fun getEvents(year: Int, month: Int) =
                if (fail) Result.failure(Exception("boom")) else Result.success(listOf(fakeEvent(15)))
            override suspend fun getYearEvents(year: Int) = Result.success(emptyList<CalendarEvent>())
        }
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.events.size)

        fail = true
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.events.size, "a failed refresh must not blank the calendar")
        assertTrue(vm.uiState.value.monthLoadFailed)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `retryMonth fetches again after a failure`() = runTest {
        val repo = CountingRepo(monthFails = true)
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.retryMonth()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, repo.monthFetchCount)
    }

    // Without this the list view would keep showing pre-refresh data after the
    // month view had already been brought up to date.
    @Test
    fun `a refresh in calendar view makes the year list reload on the next switch`() = runTest {
        val repo = CountingRepo(yearEvents = listOf(fakeEvent(1)))
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.switchView(ViewMode.LIST)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repo.yearFetchCount)

        vm.switchView(ViewMode.CALENDAR)
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.yearEventsLoaded)

        vm.switchView(ViewMode.LIST)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, repo.yearFetchCount)
    }

    @Test
    fun `a refresh in list view reloads the year straight away`() = runTest {
        val repo = CountingRepo(yearEvents = listOf(fakeEvent(1)))
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.switchView(ViewMode.LIST)
        dispatcher.scheduler.advanceUntilIdle()

        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, repo.yearFetchCount)
    }

    @Test
    fun `a failed year load reports the failure`() = runTest {
        val repo = CountingRepo(yearFails = true)
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.switchView(ViewMode.LIST)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.yearLoadFailed)
        assertFalse(vm.uiState.value.isYearLoading)
    }

    @Test
    fun `previousMonth decrements month correctly`() = runTest {
        val repo = CountingRepo()
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val initialMonth = vm.uiState.value.month
        vm.previousMonth()
        dispatcher.scheduler.advanceUntilIdle()

        val expected = if (initialMonth == 1) 12 else initialMonth - 1
        assertEquals(expected, vm.uiState.value.month)
    }

    @Test
    fun `selectDay toggles so a second tap deselects`() = runTest {
        val repo = CountingRepo()
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectDay(5)
        assertEquals(5, vm.uiState.value.selectedDay)
        vm.selectDay(5)
        assertNull(vm.uiState.value.selectedDay)
    }

    @Test
    fun `selectEvent sets selectedEvent and dismissEventDetail clears it`() = runTest {
        val repo = CountingRepo(events = listOf(fakeEvent(10)))
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectEvent(fakeEvent(10))
        assertNotNull(vm.uiState.value.selectedEvent)

        vm.dismissEventDetail()
        assertNull(vm.uiState.value.selectedEvent)
    }

    @Test
    fun `switchView to LIST loads year events and sets yearEventsLoaded`() = runTest {
        val repo = CountingRepo(yearEvents = listOf(fakeEvent(1)))
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.switchView(ViewMode.LIST)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ViewMode.LIST, vm.uiState.value.viewMode)
        assertEquals(1, repo.yearFetchCount)
        assertTrue(vm.uiState.value.yearEventsLoaded)
        assertEquals(1, vm.uiState.value.yearEvents.size)
    }

    @Test
    fun `switchView to LIST does not refetch when already loaded`() = runTest {
        val repo = CountingRepo(yearEvents = listOf(fakeEvent(1)))
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.switchView(ViewMode.LIST)
        dispatcher.scheduler.advanceUntilIdle()
        vm.switchView(ViewMode.CALENDAR)
        vm.switchView(ViewMode.LIST)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repo.yearFetchCount)
    }

    @Test
    fun `switchView to LIST sets yearEventsLoaded even when year has no events`() = runTest {
        val repo = CountingRepo()
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.switchView(ViewMode.LIST)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.yearEventsLoaded)
        assertEquals(0, vm.uiState.value.yearEvents.size)
    }
}
