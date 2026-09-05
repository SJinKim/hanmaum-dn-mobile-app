package com.hanmaum.dn.mobile.features.ministry

import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListUiState
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListViewModel
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Removed in #106 when the v2 redesign replaced this feature, and never
 * rewritten (#117).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MinistryListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeMinistryRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeMinistryRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun itStartsLoadingBeforeAnythingArrives() = runTest {
        val vm = MinistryListViewModel(repo)
        assertIs<MinistryListUiState.Loading>(vm.uiState.value)
    }

    @Test
    fun aLoadedListReachesTheState() = runTest {
        repo.ministriesResult = Result.success(
            listOf(FakeMinistryRepository.ministry("m1"), FakeMinistryRepository.ministry("m2", "찬양팀")),
        )
        val vm = MinistryListViewModel(repo)
        advanceUntilIdle()

        val state = assertIs<MinistryListUiState.Success>(vm.uiState.value)
        assertEquals(2, state.ministries.size)
        assertEquals("찬양팀", state.ministries[1].name)
    }

    @Test
    fun itAsksOnlyForActiveMinistries() = runTest {
        // The list is not supposed to show deactivated 사역; the server filters
        // when asked to, and this is the caller's half of that bargain.
        MinistryListViewModel(repo)
        advanceUntilIdle()
        assertEquals(true, repo.lastActiveOnly)
    }

    @Test
    fun anEmptyListIsSuccessNotError() = runTest {
        repo.ministriesResult = Result.success(emptyList())
        val vm = MinistryListViewModel(repo)
        advanceUntilIdle()

        val state = assertIs<MinistryListUiState.Success>(vm.uiState.value)
        assertTrue(state.ministries.isEmpty())
    }

    @Test
    fun aFailureBecomesAnErrorCarryingTheMessage() = runTest {
        repo.ministriesResult = Result.failure(IllegalStateException("offline"))
        val vm = MinistryListViewModel(repo)
        advanceUntilIdle()

        assertEquals("offline", assertIs<MinistryListUiState.Error>(vm.uiState.value).message)
    }

    @Test
    fun aFailureWithoutAMessageStillReadsAsAnError() = runTest {
        repo.ministriesResult = Result.failure(IllegalStateException())
        val vm = MinistryListViewModel(repo)
        advanceUntilIdle()

        assertTrue(assertIs<MinistryListUiState.Error>(vm.uiState.value).message.isNotBlank())
    }

    @Test
    fun retryingAfterAFailureLoadsAgain() = runTest {
        repo.ministriesResult = Result.failure(IllegalStateException("offline"))
        val vm = MinistryListViewModel(repo)
        advanceUntilIdle()
        assertIs<MinistryListUiState.Error>(vm.uiState.value)

        repo.ministriesResult = Result.success(listOf(FakeMinistryRepository.ministry()))
        vm.loadMinistries()
        advanceUntilIdle()

        assertEquals(1, assertIs<MinistryListUiState.Success>(vm.uiState.value).ministries.size)
    }
}
