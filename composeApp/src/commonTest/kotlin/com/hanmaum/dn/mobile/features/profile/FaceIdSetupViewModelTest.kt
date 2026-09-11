package com.hanmaum.dn.mobile.features.profile

import com.hanmaum.dn.mobile.core.data.repository.AuthPreferencesImpl
import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.security.FakeSecureStore
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.security.BiometricAvailability
import com.hanmaum.dn.mobile.core.security.FakeBiometricVault
import com.hanmaum.dn.mobile.core.security.VaultResult
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.profile.presentation.FaceIdSetupViewModel
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Answers who is signed in — or fails, the way a dropped connection does. */
private class SessionMemberRepository(var publicId: String? = "member-1") : MemberRepository {
    var calls = 0
        private set

    override suspend fun getMyProfile(): Result<MemberResponse> {
        calls++
        val id = publicId ?: return Result.failure(IllegalStateException("offline"))
        return Result.success(MemberResponse(publicId = id, firstName = "서진", lastName = "김", status = MemberStatus.ACTIVE))
    }

    override suspend fun updateMyProfile(
        phoneNumber: String?, profileImageUrl: String?, birthDate: String?,
        street: String?, houseNumber: String?, zipCode: String?, city: String?,
    ): Result<MemberResponse> = getMyProfile()
}

/**
 * Switching Face ID on from 설정. No password is asked for: the member is
 * already signed in, so what gets sealed is the refresh token that is already
 * in hand (#200).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FaceIdSetupViewModelTest {

    private val settings = MapSettings()
    private val authPreferences = AuthPreferencesImpl(settings)
    private val tokenStorage = TokenStorageImpl(FakeSecureStore(), settings)
    private val vault = FakeBiometricVault()
    private val members = SessionMemberRepository()

    private fun viewModel() = FaceIdSetupViewModel(tokenStorage, authPreferences, members)

    private suspend fun enable(vm: FaceIdSetupViewModel) =
        vm.enable(vault, "title", "subtitle", "cancel")

    @Test
    fun enablingSealsTheRefreshTokenAndNeedsNoPassword() = runTest {
        tokenStorage.saveRefreshToken("refresh-abc")
        val vm = viewModel()

        enable(vm)

        assertTrue(vm.uiState.value.enabled)
        assertTrue(authPreferences.isBiometricEnabled())
        assertEquals("refresh-abc", vault.sealed)
    }

    @Test
    fun cancellingThePromptLeavesTheSwitchOff() = runTest {
        tokenStorage.saveRefreshToken("refresh-abc")
        vault.nextResult = VaultResult.Cancelled
        val vm = viewModel()

        enable(vm)

        assertFalse(vm.uiState.value.enabled)
        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(vault.sealed)
        assertNull(vm.uiState.value.error, "backing out is a choice, not an error")
    }

    @Test
    fun withoutABiometricEnrolmentNothingIsSealed() = runTest {
        tokenStorage.saveRefreshToken("refresh-abc")
        vault.available = false
        val vm = viewModel()

        enable(vm)

        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(vault.sealed)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun withoutARefreshTokenTheSwitchRefusesRatherThanArmingSomethingBroken() = runTest {
        val vm = viewModel()

        enable(vm)

        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(vault.sealed)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun theArmingRecordsTheMemberOfTheLiveSession() = runTest {
        // The owner used to come from a value only the password form wrote, so an
        // auto-login through the splash armed Face ID for nobody — or for whoever
        // had typed a password last.
        tokenStorage.saveRefreshToken("refresh-abc")
        members.publicId = "member-7"

        enable(viewModel())

        assertEquals("member-7", authPreferences.biometricMemberId())
    }

    @Test
    fun withoutKnowingTheMemberNothingIsSealedAndNoPromptIsRaised() = runTest {
        // Asked before the prompt: a member who has just looked at the camera
        // should not then learn the setup failed on a network call.
        tokenStorage.saveRefreshToken("refresh-abc")
        members.publicId = null
        val vm = viewModel()

        enable(vm)

        assertNull(vault.sealed)
        assertFalse(authPreferences.isBiometricEnabled())
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun aRefusedPermissionIsNamedAsSuchNotAsAMissingFaceId() = runTest {
        // The switch used to say "no biometrics on this device" here, which is
        // false and left the member without a way back (#229).
        tokenStorage.saveRefreshToken("refresh-abc")
        vault.available = false
        vault.unavailableBecause = BiometricAvailability.DENIED
        val vm = viewModel()

        enable(vm)

        val error = vm.uiState.value.error
        assertNotNull(error)
        assertTrue("설정" in error, "points to the settings where it is allowed")
        assertFalse("등록된 생체 인증이 없습니다" in error)
    }

    @Test
    fun switchingItOffEmptiesTheVault() = runTest {
        tokenStorage.saveRefreshToken("refresh-abc")
        val vm = viewModel()
        enable(vm)

        vm.disable(vault)

        assertFalse(vm.uiState.value.enabled)
        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(vault.sealed)
        assertTrue(vault.cleared)
    }
}
