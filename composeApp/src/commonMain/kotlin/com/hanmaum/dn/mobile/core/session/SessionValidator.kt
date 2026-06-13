package com.hanmaum.dn.mobile.core.session

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository

/**
 * Probes whether the stored session is still good. Used right after a Face ID
 * unlock: the lock is only a local privacy gate and knows nothing about token
 * validity, so after a long background/idle the offline token may already be
 * dead even though biometrics succeeded.
 *
 * The probe hits an authed endpoint ([MemberRepository.getMyProfile]). If the
 * token is expired/revoked, the request's 401 drives Ktor's refresh hook, which
 * — on a definitive rejection — routes through [SessionManager] and signs the
 * user out cleanly. A transient network failure simply returns false without
 * tearing down the session, so flaky connectivity never logs anyone out.
 */
class SessionValidator(
    private val tokenStorage: TokenStorage,
    private val memberRepository: MemberRepository,
) {
    suspend fun isSessionValid(): Boolean {
        if (tokenStorage.getAccessToken().isNullOrBlank()) return false
        return memberRepository.getMyProfile().isSuccess
    }
}
