package com.hanmaum.dn.mobile.core.domain.repository

/**
 * Persists the user's location-sharing decision so the attendance geofence prompt
 * is asked once (not on every Home visit) and can be toggled later from Profile.
 */
interface LocationPreferences {
    /** Whether the user opted into location sharing for attendance check-in. */
    fun isSharingEnabled(): Boolean
    fun setSharingEnabled(value: Boolean)

    /** Whether the Home rationale prompt has already been shown and answered. */
    fun isPromptDismissed(): Boolean
    fun setPromptDismissed(value: Boolean)

    /**
     * Drops both decisions. The consent belongs to the member who gave it, so
     * it must not survive into the next session on a shared device.
     */
    fun clear()
}
