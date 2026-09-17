package com.hanmaum.dn.mobile.features.training

import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository.Companion.myApplication
import com.hanmaum.dn.mobile.features.training.domain.model.CancelResult
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplicationStatus
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import com.hanmaum.dn.mobile.features.training.presentation.detail.CancelOutcome
import com.hanmaum.dn.mobile.features.training.presentation.myapplications.MyApplicationsViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class MyApplicationsViewModelTest {

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
    fun itAsksTheServerForTheMembersOwnApplicationsOnStart() = runTest {
        MyApplicationsViewModel(repo)
        advanceUntilIdle()

        assertEquals(1, repo.myApplicationsCalls)
    }

    @Test
    fun itKeepsTheServerOrderInsteadOfSortingAgain() = runTest {
        repo.myApplicationsResult = TrainingResult.Success(
            listOf(
                myApplication(trainingPublicId = "newest", trainingName = "제자훈련"),
                myApplication(trainingPublicId = "oldest", trainingName = "새가족반"),
            ),
        )
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        assertEquals(listOf("newest", "oldest"), viewModel.uiState.value.applications.map { it.trainingPublicId })
    }

    @Test
    fun itTreatsNoApplicationsAsAnEmptyListAndNotAsAnError() = runTest {
        repo.myApplicationsResult = TrainingResult.Success(emptyList())
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.applications.isEmpty())
        assertFalse(state.hasError)
        assertFalse(state.isUnavailable)
        assertFalse(state.isLoading)
    }

    @Test
    fun itReportsTheExternalApiAsUnavailableRatherThanAsAnError() = runTest {
        repo.myApplicationsResult = TrainingResult.Unavailable
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isUnavailable)
        assertFalse(state.hasError)
        assertTrue(state.applications.isEmpty())
    }

    @Test
    fun itReportsAnyOtherFailureAsAnError() = runTest {
        repo.myApplicationsResult = TrainingResult.Failed
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasError)
        assertFalse(viewModel.uiState.value.isUnavailable)
    }

    @Test
    fun itAsksAgainWhenTheMemberRetries() = runTest {
        repo.myApplicationsResult = TrainingResult.Failed
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        repo.myApplicationsResult = TrainingResult.Success(listOf(myApplication()))
        viewModel.load()
        advanceUntilIdle()

        assertEquals(2, repo.myApplicationsCalls)
        assertFalse(viewModel.uiState.value.hasError)
        assertEquals(1, viewModel.uiState.value.applications.size)
    }

    @Test
    fun itOffersToCancelOnlyAnApplicationThatIsStillRunning() = runTest {
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        viewModel.openCancelDialog(myApplication(status = TrainingApplicationStatus.COMPLETED))
        assertNull(viewModel.uiState.value.cancelPrompt)

        viewModel.openCancelDialog(myApplication(status = TrainingApplicationStatus.APPLIED))
        assertNotNull(viewModel.uiState.value.cancelPrompt)
    }

    @Test
    fun itCancelsTheApplicationTheTappedRowBelongsTo() = runTest {
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        viewModel.openCancelDialog(myApplication(trainingPublicId = "the second one"))
        viewModel.confirmCancel()
        advanceUntilIdle()

        assertEquals("the second one", repo.lastCancelId)
    }

    @Test
    fun itClosesTheDialogAndReloadsWhenTheCancellationWentThrough() = runTest {
        repo.cancelResult = CancelResult.Success(null)
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        viewModel.openCancelDialog(myApplication())
        viewModel.confirmCancel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.cancelPrompt)
        assertEquals(2, repo.myApplicationsCalls)
    }

    @Test
    fun itKeepsTheDialogOpenWithTheReasonWhenTheServerRefuses() = runTest {
        repo.cancelResult = CancelResult.NotCancellable
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        viewModel.openCancelDialog(myApplication())
        viewModel.confirmCancel()
        advanceUntilIdle()

        val prompt = viewModel.uiState.value.cancelPrompt
        assertNotNull(prompt)
        assertEquals(CancelOutcome.NotCancellable, prompt.outcome)
        assertFalse(prompt.isCancelling)
    }

    @Test
    fun itReloadsAfterTheMemberClosesAnApplicationThatWasAlreadyGone() = runTest {
        repo.cancelResult = CancelResult.NotFound
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        viewModel.openCancelDialog(myApplication())
        viewModel.confirmCancel()
        advanceUntilIdle()
        viewModel.dismissCancelDialog()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.cancelPrompt)
        assertEquals(2, repo.myApplicationsCalls)
    }

    @Test
    fun itRefusesToCloseTheDialogWhileTheCallIsStillRunning() = runTest {
        val viewModel = MyApplicationsViewModel(repo)
        advanceUntilIdle()

        viewModel.openCancelDialog(myApplication())
        viewModel.confirmCancel()
        // No advanceUntilIdle: the cancellation is in flight.
        viewModel.dismissCancelDialog()

        assertNotNull(viewModel.uiState.value.cancelPrompt)
    }
}
