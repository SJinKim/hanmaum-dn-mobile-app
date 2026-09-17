package com.hanmaum.dn.mobile.features.ministry

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
import kotlin.test.assertIs

/**
 * The detail screen.
 *
 * This file used to cover a self-registration flow in depth — #117 added those tests because
 * the flow had none. They passed for months against a fake that answered success, while the
 * real endpoint never existed (#248). That is the lesson the single-call test below guards:
 * a hand-written fake proves the ViewModel's logic, never that anything is on the other end.
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

    @Test
    fun theDetailArrivesForTheMinistryItWasOpenedWith() = runTest {
        val vm = vm()
        advanceUntilIdle()

        assertEquals("m1", repo.lastDetailId)
        assertEquals("난민 사역", success(vm).detail.name)
    }

    @Test
    fun openingTheDetailMakesExactlyOneRequest() = runTest {
        // The second one used to ask ministries/{id}/registrations/me, which no server ever
        // answered; its 404 was read as "not registered" and offered 신청하기 anyway (#248).
        vm()
        advanceUntilIdle()

        assertEquals(1, repo.detailCalls)
    }

    @Test
    fun aFailedDetailIsAnError() = runTest {
        repo.detailResult = Result.failure(IllegalStateException("gone"))
        val vm = vm()
        advanceUntilIdle()

        assertEquals("gone", assertIs<MinistryDetailUiState.Error>(vm.uiState.value).message)
    }

    @Test
    fun retryingAsksAgain() = runTest {
        repo.detailResult = Result.failure(IllegalStateException("offline"))
        val vm = vm()
        advanceUntilIdle()

        repo.detailResult = Result.success(FakeMinistryRepository.detail())
        vm.load()
        advanceUntilIdle()

        assertEquals(2, repo.detailCalls)
        assertEquals("난민 사역", success(vm).detail.name)
    }
}
