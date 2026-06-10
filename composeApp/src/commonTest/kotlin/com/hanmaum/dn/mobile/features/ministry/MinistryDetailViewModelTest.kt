package com.hanmaum.dn.mobile.features.ministry

import com.hanmaum.dn.mobile.features.ministry.domain.model.Contact
import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailUiState
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailViewModel
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MinistryDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val sample = MinistryDetail(
        publicId = "m1", title = "난민 사역", subtitle = "사랑을 나누는 사역",
        about = "소개", requirements = emptyList(), schedules = emptyList(),
        contacts = listOf(Contact("팀장", "김영원 권사님")), imageUrl = null, isActive = true,
    )

    private fun repo(result: Result<MinistryDetail>) = object : MinistryRepository {
        override suspend fun getMinistries(activeOnly: Boolean): Result<List<Ministry>> =
            Result.success(emptyList())
        override suspend fun getMinistryDetail(publicId: String) = result
    }

    @Test
    fun `success emits Success with detail`() = runTest {
        val vm = MinistryDetailViewModel("m1", repo(Result.success(sample)))
        dispatcher.scheduler.advanceUntilIdle()

        val state = assertIs<MinistryDetailUiState.Success>(vm.uiState.value)
        assertEquals("난민 사역", state.detail.title)
        assertEquals(1, state.detail.contacts.size)
    }

    @Test
    fun `failure emits Error`() = runTest {
        val vm = MinistryDetailViewModel("m1", repo(Result.failure(RuntimeException("오류"))))
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<MinistryDetailUiState.Error>(vm.uiState.value)
    }
}
