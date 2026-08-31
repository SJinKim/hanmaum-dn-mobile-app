package com.hanmaum.dn.mobile.features.pending

import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.pending.presentation.PendingViewModel
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
import kotlin.test.assertNull

private class FakeMemberRepository(var status: MemberStatus) : MemberRepository {
    override suspend fun getMyProfile(): Result<MemberResponse> = Result.success(
        MemberResponse(publicId = "p1", firstName = "서진", lastName = "김", status = status),
    )

    override suspend fun updateMyProfile(
        phoneNumber: String?, profileImageUrl: String?, birthDate: String?,
        street: String?, houseNumber: String?, zipCode: String?, city: String?,
    ): Result<MemberResponse> = getMyProfile()
}

private class FakeTokenStorage : TokenStorage {
    var cleared = false
    override fun saveAccessToken(token: String) {}
    override fun getAccessToken(): String? = "token"
    override fun saveRefreshToken(token: String?) {}
    override fun getRefreshToken(): String? = null
    override fun clear() { cleared = true }
    override fun setKeepSignedIn(value: Boolean) {}
    override fun isKeepSignedIn(): Boolean = true
    override fun setBiometricEnabled(value: Boolean) {}
    override fun isBiometricEnabled(): Boolean = false
}

@OptIn(ExperimentalCoroutinesApi::class)
class PendingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(status: MemberStatus) =
        PendingViewModel(FakeMemberRepository(status), FakeTokenStorage())

    @Test
    fun approvalSendsTheMemberHome() = runTest(dispatcher) {
        val vm = vm(MemberStatus.ACTIVE)
        vm.onCheckStatusClicked(); advanceUntilIdle()
        assertEquals(NavRoute.Home, vm.uiState.value.navigateTo)
    }

    @Test
    fun anUndecidedApplicationKeepsWaiting() = runTest(dispatcher) {
        val vm = vm(MemberStatus.PENDING)
        vm.onCheckStatusClicked(); advanceUntilIdle()
        assertNull(vm.uiState.value.navigateTo)
    }

    @Test
    fun aRefusalWhileWaitingLeavesThisScreen() = runTest(dispatcher) {
        // The case from the flow: someone sits here, the decision lands. Before
        // this the button answered "please wait" for ever.
        val vm = vm(MemberStatus.REJECTED)
        vm.onCheckStatusClicked(); advanceUntilIdle()
        assertEquals(NavRoute.Rejected, vm.uiState.value.navigateTo)
    }

    @Test
    fun aStatusThisBuildDoesNotKnowDoesNotClaimRejection() = runTest(dispatcher) {
        // UNKNOWN must not be shown the refusal text — it is not one.
        val vm = vm(MemberStatus.UNKNOWN)
        vm.onCheckStatusClicked(); advanceUntilIdle()
        assertNull(vm.uiState.value.navigateTo)
    }
}
