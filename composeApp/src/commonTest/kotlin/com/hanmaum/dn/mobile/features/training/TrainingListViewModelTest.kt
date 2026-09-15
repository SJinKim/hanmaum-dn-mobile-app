package com.hanmaum.dn.mobile.features.training

import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import com.hanmaum.dn.mobile.features.training.presentation.list.TrainingListViewModel
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeTrainingRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeTrainingRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun itStartsLoadingBeforeAnythingArrives() = runTest {
        val vm = TrainingListViewModel(repo)

        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun theListKeepsTheServerOrder() = runTest {
        // Open trainings first is the server's decision; re-sorting here would undo it.
        repo.listResult = TrainingResult.Success(
            listOf(
                FakeTrainingRepository.training("t9", open = true),
                FakeTrainingRepository.training("t1", open = false),
            ),
        )

        val vm = TrainingListViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("t9", "t1"), state.trainings.map { it.publicId })
    }

    @Test
    fun unavailableIsNotAnError() = runTest {
        repo.listResult = TrainingResult.Unavailable

        val vm = TrainingListViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isUnavailable)
        assertFalse(state.hasError)
        assertTrue(state.trainings.isEmpty())
    }

    @Test
    fun aFailureShowsTheErrorAndARetryClearsIt() = runTest {
        repo.listResult = TrainingResult.Failed
        val vm = TrainingListViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasError)

        repo.listResult = TrainingResult.Success(listOf(FakeTrainingRepository.training()))
        vm.load()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.hasError)
        assertEquals(1, state.trainings.size)
        assertEquals(2, repo.listCalls)
    }
}
