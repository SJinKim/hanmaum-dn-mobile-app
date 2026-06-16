package com.hanmaum.dn.mobile.core.domain.repository

/**
 * Persists the user's last successful attendance check-in locally.
 *
 * PR#80 removed the member-level `/attendance/logs/me` endpoint (PII minimization),
 * so the app can no longer ask the server "did I already check in today?". Persisting
 * the check-in here lets the screen restore the already-checked-in state after an app
 * restart, preventing a second check-in attempt for the same definition on the same day.
 * The server's unique constraint (409 Conflict) remains the authoritative guard.
 */
interface AttendancePreferences {
    /** True if a successful check-in is recorded for this definition on this date. */
    fun isCheckedIn(definitionId: String, date: String): Boolean

    /** Records a successful check-in for this definition/date. */
    fun markCheckedIn(definitionId: String, date: String)
}
