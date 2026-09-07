package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.AttendancePreferences
import com.hanmaum.dn.mobile.core.domain.repository.RecordedAttendanceCheckIn
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AttendancePreferencesImpl(private val settings: Settings) : AttendancePreferences {

    private val _lastCheckIn = MutableStateFlow(
        settings.getStringOrNull(KEY_LAST_CHECK_IN)?.toRecord(),
    )
    override val lastCheckIn: StateFlow<RecordedAttendanceCheckIn?> = _lastCheckIn.asStateFlow()

    override fun isCheckedIn(definitionId: String, date: String): Boolean =
        lastCheckIn.value == RecordedAttendanceCheckIn(definitionId, date)

    override fun markCheckedIn(definitionId: String, date: String) {
        settings.putString(KEY_LAST_CHECK_IN, key(definitionId, date))
        _lastCheckIn.value = RecordedAttendanceCheckIn(definitionId, date)
    }

    override fun clearCheckedIn(definitionId: String, date: String) {
        if (!isCheckedIn(definitionId, date)) return
        settings.remove(KEY_LAST_CHECK_IN)
        _lastCheckIn.value = null
    }

    private fun key(definitionId: String, date: String) = "$date|$definitionId"

    private fun String.toRecord(): RecordedAttendanceCheckIn? {
        val separator = indexOf('|')
        if (separator <= 0 || separator == lastIndex) return null
        return RecordedAttendanceCheckIn(
            definitionId = substring(separator + 1),
            date = substring(0, separator),
        )
    }

    private companion object {
        const val KEY_LAST_CHECK_IN = "attendance_last_check_in"
    }
}
