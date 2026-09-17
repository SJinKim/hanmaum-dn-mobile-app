package com.hanmaum.dn.mobile.features.training

import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository.Companion.application
import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository.Companion.course
import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository.Companion.detail
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicantPrefill
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicationField
import com.hanmaum.dn.mobile.features.training.domain.model.ApplyResult
import com.hanmaum.dn.mobile.features.training.domain.model.CancelResult
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplicationStatus
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import com.hanmaum.dn.mobile.features.training.presentation.detail.CancelOutcome
import com.hanmaum.dn.mobile.features.training.presentation.detail.FieldError
import com.hanmaum.dn.mobile.features.training.presentation.detail.FormOutcome
import com.hanmaum.dn.mobile.features.training.presentation.detail.TrainingDetailViewModel
import kotlinx.coroutines.test.TestScope
import kotlin.test.assertNotNull
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
        assertNotNull(vm.uiState.value.cancelPrompt)

        vm.dismissCancelDialog()
        assertNull(vm.uiState.value.cancelPrompt)
        assertEquals(0, repo.cancelCalls, "closing the dialog cancels nothing")
        assertEquals(TrainingApplicationStatus.APPLIED, vm.uiState.value.detail?.myApplication?.status)
    }

    // ─── Application form (#174) ─────────────────────────────────────────────

    private fun applicable(prefill: ApplicantPrefill? = FakeTrainingRepository.PREFILL) {
        repo.detailResult = TrainingResult.Success(detail(courses = listOf(course(106)), prefill = prefill))
    }

    private fun TestScope.openedForm(): TrainingDetailViewModel {
        val vm = loaded()
        advanceUntilIdle()
        vm.openApplicationForm()
        return vm
    }

    private fun TestScope.sent(vm: TrainingDetailViewModel) {
        vm.setConsent(true)
        vm.submitApplication()
        advanceUntilIdle()
    }

    @Test
    fun theFormOpensOnlyForACourseThatCanBeApplied() = runTest {
        repo.detailResult = TrainingResult.Success(detail(courses = listOf(course(105), course(106))))
        val vm = loaded()
        advanceUntilIdle()

        vm.openApplicationForm()
        assertNull(vm.uiState.value.form, "no course chosen yet")

        vm.selectCourse(106)
        vm.openApplicationForm()
        assertEquals(106, vm.uiState.value.form?.course?.externalCourseId)
    }

    @Test
    fun theFormStartsFromTheProfile() = runTest {
        applicable()

        val vm = openedForm()

        assertEquals("김한마음", vm.uiState.value.form?.values?.get(ApplicationField.NAME))
        assertEquals("1995.03.14", vm.uiState.value.form?.values?.get(ApplicationField.BIRTH_DATE))
    }

    @Test
    fun aLockedProfileValueIgnoresEdits() = runTest {
        applicable()
        val vm = openedForm()

        vm.updateField(ApplicationField.NAME, "다른 이름")

        assertEquals("김한마음", vm.uiState.value.form?.values?.get(ApplicationField.NAME))
    }

    @Test
    fun nothingIsSentWithoutConsent() = runTest {
        applicable()
        val vm = openedForm()

        vm.submitApplication()
        advanceUntilIdle()

        assertEquals(0, repo.applyCalls)
    }

    @Test
    fun aFormThatFailsValidationIsNotSent() = runTest {
        applicable(prefill = null)
        val vm = openedForm()

        sent(vm)

        assertEquals(0, repo.applyCalls)
        assertEquals(FieldError.Required, vm.uiState.value.form?.errors?.get(ApplicationField.PHONE))
    }

    @Test
    fun aSentApplicationIsConfirmedAndClosingReloadsThePage() = runTest {
        applicable()
        val vm = openedForm()

        sent(vm)

        assertEquals(1, repo.applyCalls)
        assertEquals(106, repo.lastApplyCourseId)
        assertEquals("+49 170 1234567", repo.lastApplyValues[ApplicationField.PHONE])
        assertNull(
            repo.lastApplyValues[ApplicationField.BIRTH_DATE],
            "a locked profile value is not sent; the server takes it from the profile",
        )
        assertEquals(FormOutcome.Success("큐베세 직장인/청년 반"), vm.uiState.value.form?.outcome)

        val loads = repo.detailCalls
        vm.closeApplicationForm()
        advanceUntilIdle()

        assertNull(vm.uiState.value.form)
        assertEquals(loads + 1, repo.detailCalls, "신청 현황 must show the new application")
    }

    @Test
    fun alreadyAppliedElsewhereAlsoReloadsThePage() = runTest {
        applicable()
        repo.applyResult = ApplyResult.AlreadyApplied
        val vm = openedForm()

        sent(vm)
        assertEquals(FormOutcome.AlreadyApplied, vm.uiState.value.form?.outcome)

        val loads = repo.detailCalls
        vm.closeApplicationForm()
        advanceUntilIdle()
        assertEquals(loads + 1, repo.detailCalls)
    }

    @Test
    fun unavailableKeepsTheInputForALaterTry() = runTest {
        applicable()
        repo.applyResult = ApplyResult.Unavailable
        val vm = openedForm()
        vm.updateField(ApplicationField.PHONE, "+49 171 7654321")

        sent(vm)

        val form = assertNotNull(vm.uiState.value.form)
        assertEquals(FormOutcome.Unavailable, form.outcome)
        assertEquals("+49 171 7654321", form.values[ApplicationField.PHONE])
        assertTrue(form.canSubmit)

        val loads = repo.detailCalls
        vm.closeApplicationForm()
        advanceUntilIdle()
        assertEquals(loads, repo.detailCalls, "nothing was applied, so nothing to reload")
    }

    @Test
    fun serverFieldErrorsLandOnTheirFieldsAndClearWhenEdited() = runTest {
        applicable()
        repo.applyResult = ApplyResult.Invalid(mapOf(ApplicationField.EMAIL to "올바른 이메일 주소여야 합니다."), null)
        val vm = openedForm()

        sent(vm)

        val form = assertNotNull(vm.uiState.value.form)
        assertEquals(FieldError.Server("올바른 이메일 주소여야 합니다."), form.errors[ApplicationField.EMAIL])
        assertEquals(FormOutcome.Invalid(null), form.outcome)

        vm.updateField(ApplicationField.EMAIL, "new@example.com")
        assertNull(vm.uiState.value.form?.errors?.get(ApplicationField.EMAIL))
    }

    @Test
    fun aFullCourseCannotBeSentAgain() = runTest {
        applicable()
        repo.applyResult = ApplyResult.Full
        val vm = openedForm()

        sent(vm)
        assertFalse(vm.uiState.value.form?.canSubmit ?: true)

        vm.submitApplication()
        advanceUntilIdle()
        assertEquals(1, repo.applyCalls)
    }

    @Test
    fun aSecondTapWhileSendingIsIgnored() = runTest {
        applicable()
        val vm = openedForm()
        vm.setConsent(true)

        vm.submitApplication()
        vm.submitApplication()
        advanceUntilIdle()

        assertEquals(1, repo.applyCalls)
    }

    @Test
    fun closingWhileSendingStillShowsTheApplication() = runTest {
        applicable()
        val vm = openedForm()
        vm.setConsent(true)
        vm.submitApplication()
        vm.closeApplicationForm()

        val loads = repo.detailCalls
        advanceUntilIdle()

        assertNull(vm.uiState.value.form)
        assertEquals(loads + 1, repo.detailCalls)
    }

    // ─── Cancelling (#245) ───────────────────────────────────────────────────

    private fun applied(status: TrainingApplicationStatus = TrainingApplicationStatus.APPLIED) {
        repo.detailResult = TrainingResult.Success(detail(application = application(status)))
    }

    private fun TestScope.promptOpen(status: TrainingApplicationStatus = TrainingApplicationStatus.APPLIED):
        TrainingDetailViewModel {
        applied(status)
        val vm = loaded()
        advanceUntilIdle()
        vm.openCancelDialog()
        return vm
    }

    @Test
    fun aFinishedApplicationHasNothingToCancel() = runTest {
        val vm = promptOpen(TrainingApplicationStatus.COMPLETED)
        advanceUntilIdle()

        assertNull(vm.uiState.value.cancelPrompt)
    }

    @Test
    fun aRunningTrainingIsStoppedRatherThanWithdrawn() = runTest {
        val vm = promptOpen(TrainingApplicationStatus.IN_PROGRESS)
        advanceUntilIdle()

        assertTrue(assertNotNull(vm.uiState.value.cancelPrompt).isAbort)
    }

    @Test
    fun confirmingCancelsOnceEvenOnASecondTap() = runTest {
        val vm = promptOpen()

        vm.confirmCancel()
        vm.confirmCancel()
        advanceUntilIdle()

        assertEquals(1, repo.cancelCalls)
        assertEquals("t1", repo.lastCancelId)
    }

    @Test
    fun theDialogCannotBeClosedWhileTheCallRuns() = runTest {
        val vm = promptOpen()

        vm.confirmCancel()
        vm.dismissCancelDialog()

        assertTrue(assertNotNull(vm.uiState.value.cancelPrompt).isCancelling)
        advanceUntilIdle()
    }

    @Test
    fun aCancelledApplicationClosesTheDialogAndReloadsThePage() = runTest {
        val vm = promptOpen()
        val loads = repo.detailCalls

        vm.confirmCancel()
        advanceUntilIdle()

        assertNull(vm.uiState.value.cancelPrompt)
        assertEquals(loads + 1, repo.detailCalls)
    }

    @Test
    fun aCompletedTrainingIsRefusedAndTheApplicationStays() = runTest {
        repo.cancelResult = CancelResult.NotCancellable
        val vm = promptOpen()
        val loads = repo.detailCalls

        vm.confirmCancel()
        advanceUntilIdle()

        val prompt = assertNotNull(vm.uiState.value.cancelPrompt)
        assertEquals(CancelOutcome.NotCancellable, prompt.outcome)
        assertFalse(prompt.canConfirm, "retrying cannot change a 수료")
        assertEquals(loads, repo.detailCalls)
    }

    @Test
    fun anApplicationThatIsGoneReloadsOnceTheDialogIsClosed() = runTest {
        repo.cancelResult = CancelResult.NotFound
        val vm = promptOpen()

        vm.confirmCancel()
        advanceUntilIdle()
        assertEquals(CancelOutcome.NotFound, assertNotNull(vm.uiState.value.cancelPrompt).outcome)

        val loads = repo.detailCalls
        vm.dismissCancelDialog()
        advanceUntilIdle()

        assertNull(vm.uiState.value.cancelPrompt)
        assertEquals(loads + 1, repo.detailCalls, "the application shown is gone, so the page is refetched")
    }

    @Test
    fun anUnreachableExternalApiSaysComingSoon() = runTest {
        repo.cancelResult = CancelResult.Unavailable
        val vm = promptOpen()

        vm.confirmCancel()
        advanceUntilIdle()

        assertEquals(CancelOutcome.Unavailable, assertNotNull(vm.uiState.value.cancelPrompt).outcome)
    }

    @Test
    fun aFailedCancelCanBeTriedAgain() = runTest {
        repo.cancelResult = CancelResult.Failed
        val vm = promptOpen()

        vm.confirmCancel()
        advanceUntilIdle()
        assertTrue(assertNotNull(vm.uiState.value.cancelPrompt).canConfirm)

        repo.cancelResult = CancelResult.Success(application(TrainingApplicationStatus.DROPPED))
        vm.confirmCancel()
        advanceUntilIdle()

        assertEquals(2, repo.cancelCalls)
        assertNull(vm.uiState.value.cancelPrompt)
    }
}
