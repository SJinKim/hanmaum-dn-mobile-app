package com.hanmaum.dn.mobile.features.ministry

import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListUiState
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListViewModel
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
class MinistryListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val sample = Ministry(
        publicId = "m1", title = "난민 사역", subtitle = "사랑을 나누는 사역",
        imageUrl = null, contacts = emptyList(), isActive = true,
    )

    private fun repo(result: Result<List<Ministry>>) = object : MinistryRepository {
        override suspend fun getMinistries(activeOnly: Boolean) = result
        override suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail> =
            Result.failure(NotImplementedError())
    }

    @Test
    fun `success emits Success with ministries`() = runTest {
        val vm = MinistryListViewModel(repo(Result.success(listOf(sample))))
        dispatcher.scheduler.advanceUntilIdle()

        val state = assertIs<MinistryListUiState.Success>(vm.uiState.value)
        assertEquals(1, state.ministries.size)
        assertEquals("난민 사역", state.ministries[0].title)
    }

    @Test
    fun `failure emits Error`() = runTest {
        val vm = MinistryListViewModel(repo(Result.failure(RuntimeException("네트워크 오류"))))
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<MinistryListUiState.Error>(vm.uiState.value)
    }
}
