package com.hanmaum.dn.mobile.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class RecordedAttendanceCheckIn(
    val definitionId: String,
    val date: String,
)

/**
 * Persists and publishes the user's last server-confirmed attendance check-in.
 *
 * The flow synchronizes live ViewModel instances immediately. Persistence restores
 * the state before a network refresh completes, while `/me/attendance` reconciles it
 * with server data after launch/resume. The server's unique constraint (409 Conflict)
 * remains the authoritative guard.
 */
interface AttendancePreferences {
    /**
     * The last server-confirmed check-in, shared by every attendance ViewModel.
     *
     * Home and the attendance screen own separate ViewModel instances, so a
     * synchronous getter alone leaves whichever screen is in the back stack
     * stale. The flow makes this persisted value the common in-process source
     * of truth as well.
     */
    val lastCheckIn: StateFlow<RecordedAttendanceCheckIn?>

    /** True if a successful check-in is recorded for this definition on this date. */
    fun isCheckedIn(definitionId: String, date: String): Boolean

    /** Records a successful check-in for this definition/date. */
    fun markCheckedIn(definitionId: String, date: String)

    /** Removes this exact record when a successful server refresh disproves it. */
    fun clearCheckedIn(definitionId: String, date: String)
}
