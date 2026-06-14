package com.hanmaum.dn.mobile.core.session

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single, canonical "end this session" pipeline. Every logout path — the
 * explicit Profile button, the lock screen's "use password", and a definitive
 * token rejection surfaced by the Ktor auth refresh hook — funnels through
 * [logout] so the behaviour is identical and complete every time:
 *
 *  1. Revoke the session server-side (Keycloak logout endpoint). Important for
 *     offline tokens, whose session otherwise lingers for the Offline Session
 *     Idle window (default 30 days) after the app has locally signed out.
 *  2. Clear persisted tokens so splash/login don't auto-skip on next launch.
 *  3. Drop Ktor's in-memory [BearerAuthProvider] cache — otherwise the provider
 *     replays the dead access token on the next request and a fresh login can't
 *     take effect. This is the classic "logged out but still authorized / can't
 *     log back in" bug.
 *  4. Emit on [events] so the single global collector in `App` navigates to
 *     Login exactly once.
 *
 * A [Mutex] guard collapses a burst of concurrent 401s (each independently
 * asking to log out) into a single revoke + navigation.
 */
class SessionManager(
    private val tokenStorage: TokenStorage,
    // Deferred so DI has no construction cycle with the authed HttpClient that
    // owns the BearerAuthProvider this clears.
    private val clearBearerCache: () -> Unit,
) {
    // Plain client, no auth interceptor — the logout call must not carry (or try
    // to refresh) the very token we're tearing down.
    private val logoutClient = HttpClient()

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    private val mutex = Mutex()
    private var inProgress = false

    suspend fun logout() {
        mutex.withLock {
            if (inProgress) return
            inProgress = true
        }
        try {
            revokeQuietly()
            tokenStorage.clear()
            clearBearerCache()
            _events.emit(Unit)
        } finally {
            mutex.withLock { inProgress = false }
        }
    }

    /**
     * Called by the Ktor auth refresh hook when Keycloak *definitively* rejects
     * the refresh/offline token (expired or revoked). Transient failures must
     * not call this — they would force an unnecessary sign-out.
     */
    suspend fun onRefreshRejected() = logout()

    private suspend fun revokeQuietly() {
        val refresh = tokenStorage.getRefreshToken() ?: return
        try {
            logoutClient.submitForm(
                url = "${BuildKonfig.KEYCLOAK_URL}/realms/${BuildKonfig.KEYCLOAK_REALM}/protocol/openid-connect/logout",
                formParameters = parameters {
                    append("client_id", "hanmaum-mobile")
                    append("refresh_token", refresh)
                },
            )
        } catch (_: Exception) {
            // Best-effort. If the token is already dead or the network is down,
            // the local clear below is what matters for the user experience.
        }
    }
}
