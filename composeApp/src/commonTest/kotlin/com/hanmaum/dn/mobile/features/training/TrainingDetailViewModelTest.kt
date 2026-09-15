package com.hanmaum.dn.mobile.features.training

import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository.Companion.application
import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository.Companion.course
import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository.Companion.detail
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplicationStatus
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import com.hanmaum.dn.mobile.features.training.presentation.detail.TrainingDetailViewModel
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeTrainingRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeTrainingRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun loaded(): TrainingDetailViewModel = TrainingDetailViewModel("t1", repo)

    @Test
    fun itAsksForTheTrainingItWasOpenedWith() = runTest {
        loaded()
        advanceUntilIdle()

        assertEquals("t1", repo.lastDetailId)
    }

    @Test
    fun theOnlyOpenCourseIsChosenForTheMember() = runTest {
        repo.detailResult = TrainingResult.Success(detail(courses = listOf(course(106))))

        val vm = loaded()
        advanceUntilIdle()

        assertEquals(106, vm.uiState.value.selectedCourseId)
        assertTrue(vm.uiState.value.canApply)
    }

    @Test
    fun theOnlyOpenCourseIsNotChosenWhenTheMemberMayNotApply() = runTest {
        repo.detailResult = TrainingResult.Success(detail(courses = listOf(course(105, eligible = false))))

        val vm = loaded()
        advanceUntilIdle()

        assertNull(vm.uiState.value.selectedCourseId)
        assertFalse(vm.uiState.value.canApply)
    }

    @Test
    fun severalOpenCoursesWaitForAChoice() = runTest {
        repo.detailResult = TrainingResult.Success(detail(courses = listOf(course(105), course(106))))

        val vm = loaded()
        advanceUntilIdle()
        assertNull(vm.uiState.value.selectedCourseId)
        assertFalse(vm.uiState.value.canApply)

        vm.selectCourse(106)

        assertEquals(106, vm.uiState.value.selectedCourse?.externalCourseId)
        assertTrue(vm.uiState.value.canApply)
    }

    @Test
    fun aCourseTheMemberMayNotApplyToCannotBeChosen() = runTest {
        repo.detailResult = TrainingResult.Success(
            detail(courses = listOf(course(105, eligible = false), course(106))),
        )
        val vm = loaded()
        advanceUntilIdle()

        vm.selectCourse(105)
        assertNull(vm.uiState.value.selectedCourseId)

        vm.selectCourse(106)
        vm.selectCourse(105)
        assertEquals(106, vm.uiState.value.selectedCourseId, "an ineligible tap must not replace a valid choice")
    }

    @Test
    fun noOpenCourseMeansNothingToApplyFor() = runTest {
        repo.detailResult = TrainingResult.Success(detail(courses = emptyList()))

        val vm = loaded()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.canApply)
    }

    @Test
    fun aRunningApplicationIsNotOfferedAgain() = runTest {
        // The server answers ALREADY_APPLIED for these three; the button must not invite that.
        for (status in listOf(
            TrainingApplicationStatus.APPLIED,
            TrainingApplicationStatus.ENROLLED,
            TrainingApplicationStatus.IN_PROGRESS,
        )) {
            repo.detailResult = TrainingResult.Success(
                detail(courses = listOf(course(106)), application = application(status)),
            )
            val vm = loaded()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.hasActiveApplication, "$status")
            assertFalse(vm.uiState.value.canApply, "$status")
        }
    }

    @Test
    fun aFinishedApplicationMayApplyAgain() = runTest {
        // COMPLETED may reapply, DROPPED is reopened by the server (hanmaum-dn-server#167).
        for (status in listOf(TrainingApplicationStatus.COMPLETED, TrainingApplicationStatus.DROPPED)) {
            repo.detailResult = TrainingResult.Success(
                detail(courses = listOf(course(106)), application = application(status)),
            )
            val vm = loaded()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.hasActiveApplication, "$status")
            assertTrue(vm.uiState.value.canApply, "$status")
        }
    }

    @Test
    fun unavailableIsNotAnError() = runTest {
        repo.detailResult = TrainingResult.Unavailable

        val vm = loaded()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isUnavailable)
        assertFalse(vm.uiState.value.hasError)
        assertNull(vm.uiState.value.detail)
    }

    @Test
    fun aFailureCanBeRetried() = runTest {
        repo.detailResult = TrainingResult.Failed
        val vm = loaded()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasError)

        repo.detailResult = TrainingResult.Success(detail(courses = listOf(course(106))))
        vm.load()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.hasError)
        assertEquals(106, vm.uiState.value.selectedCourseId)
    }

    @Test
    fun theCancelDialogOpensAndClosesWithoutTouchingTheApplication() = runTest {
        repo.detailResult = TrainingResult.Success(
            detail(courses = listOf(course(106)), application = application(TrainingApplicationStatus.APPLIED)),
        )
        val vm = loaded()
        advanceUntilIdle()

        vm.openCancelDialog()
        assertTrue(vm.uiState.value.showCancelDialog)

        vm.dismissCancelDialog()
        assertFalse(vm.uiState.value.showCancelDialog)
        assertEquals(TrainingApplicationStatus.APPLIED, vm.uiState.value.detail?.myApplication?.status)
    }
}
