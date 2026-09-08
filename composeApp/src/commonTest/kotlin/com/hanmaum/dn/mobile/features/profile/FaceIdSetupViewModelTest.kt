package com.hanmaum.dn.mobile.features.profile

import com.hanmaum.dn.mobile.core.data.repository.AuthPreferencesImpl
import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.security.FakeBiometricVault
import com.hanmaum.dn.mobile.core.security.VaultResult
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

/**
 * Switching Face ID on from 설정. No password is asked for: the member is
 * already signed in, so what gets sealed is the refresh token that is already
 * in hand (#200).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FaceIdSetupViewModelTest {

    private val settings = MapSettings()
    private val authPreferences = AuthPreferencesImpl(settings)
    private val tokenStorage = TokenStorageImpl(settings)
    private val vault = FakeBiometricVault()

    private fun viewModel() = FaceIdSetupViewModel(tokenStorage, authPreferences)

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
