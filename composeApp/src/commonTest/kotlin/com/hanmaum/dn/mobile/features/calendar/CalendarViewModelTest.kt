package com.hanmaum.dn.mobile.features.calendar

import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent
import com.hanmaum.dn.mobile.features.calendar.domain.repository.CalendarRepository
import com.hanmaum.dn.mobile.features.calendar.presentation.CalendarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest  fun tearDown() { Dispatchers.resetMain() }

    private fun fakeEvent(day: Int) = CalendarEvent(
        id = "evt-$day", title = "예배 $day", description = null,
        location = "본당",
        startDate = "2026-05-${day.toString().padStart(2, '0')}",
        endDate   = "2026-05-${day.toString().padStart(2, '0')}",
        isAllDay  = true,
    )

    @Test
    fun `loads events for current month on init`() = runTest {
        val repo = object : CalendarRepository {
            override suspend fun getEvents(year: Int, month: Int) = Result.success(listOf(fakeEvent(15)))
        }
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.events.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `previousMonth decrements month correctly`() = runTest {
        val repo = object : CalendarRepository {
            override suspend fun getEvents(year: Int, month: Int) = Result.success(emptyList<CalendarEvent>())
        }
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val initialMonth = vm.uiState.value.month
        vm.previousMonth()
        dispatcher.scheduler.advanceUntilIdle()

        val expected = if (initialMonth == 1) 12 else initialMonth - 1
        assertEquals(expected, vm.uiState.value.month)
    }

    @Test
    fun `selectDay toggles — second tap deselects`() = runTest {
        val repo = object : CalendarRepository {
            override suspend fun getEvents(year: Int, month: Int) = Result.success(emptyList<CalendarEvent>())
        }
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectDay(5)
        assertEquals(5, vm.uiState.value.selectedDay)
        vm.selectDay(5)
        assertNull(vm.uiState.value.selectedDay)
    }

    @Test
    fun `selectEvent sets selectedEvent, dismissEventDetail clears it`() = runTest {
        val repo = object : CalendarRepository {
            override suspend fun getEvents(year: Int, month: Int) = Result.success(listOf(fakeEvent(10)))
        }
        val vm = CalendarViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectEvent(fakeEvent(10))
        assertNotNull(vm.uiState.value.selectedEvent)

        vm.dismissEventDetail()
        assertNull(vm.uiState.value.selectedEvent)
    }
}
