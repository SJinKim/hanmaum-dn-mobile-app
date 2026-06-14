package com.hanmaum.dn.mobile.core.session

import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.RefreshTokenPersistence
import com.hanmaum.dn.mobile.core.security.RefreshTokenVault
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionValidatorTest {

    private val member = MemberResponse(
        publicId = "p1",
        firstName = "Test",
        lastName = "User",
        status = MemberStatus.ACTIVE,
    )

    private class FakeMemberRepository(
        private val result: Result<MemberResponse>,
    ) : MemberRepository {
        var profileCalls = 0
            private set

        override suspend fun getMyProfile(): Result<MemberResponse> {
            profileCalls++
            return result
        }

        override suspend fun updateMyProfile(
            phoneNumber: String?,
            profileImageUrl: String?,
            street: String?,
            zipCode: String?,
            city: String?,
        ): Result<MemberResponse> = result
    }

    private class NoopPersistence : RefreshTokenPersistence {
        override fun isDeviceSecured() = true
        override fun hasStored() = false
        override fun store(token: String) {}
        override fun delete() {}
    }

    private fun storageWith(accessToken: String?): TokenStorage =
        TokenStorageImpl(MapSettings(), RefreshTokenVault(NoopPersistence())).apply {
            if (accessToken != null) saveAccessToken(accessToken)
        }

    @Test
    fun `invalid when no access token is stored`() = runTest {
        val repo = FakeMemberRepository(Result.success(member))
        val validator = SessionValidator(storageWith(null), repo)

        assertFalse(validator.isSessionValid())
        // Must short-circuit without an unnecessary network probe.
        assertEquals(0, repo.profileCalls)
    }

    @Test
    fun `valid when token present and profile probe succeeds`() = runTest {
        val repo = FakeMemberRepository(Result.success(member))
        val validator = SessionValidator(storageWith("access-token"), repo)

        assertTrue(validator.isSessionValid())
        assertEquals(1, repo.profileCalls)
    }

    @Test
    fun `invalid when token present but profile probe fails`() = runTest {
        val repo = FakeMemberRepository(Result.failure(RuntimeException("401")))
        val validator = SessionValidator(storageWith("access-token"), repo)

        assertFalse(validator.isSessionValid())
        assertEquals(1, repo.profileCalls)
    }
}
