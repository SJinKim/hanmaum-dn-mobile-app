package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.AttendancePreferences
import com.russhwolf.settings.Settings

class AttendancePreferencesImpl(private val settings: Settings) : AttendancePreferences {

    override fun isCheckedIn(definitionId: String, date: String): Boolean =
        settings.getStringOrNull(KEY_LAST_CHECK_IN) == key(definitionId, date)

    override fun markCheckedIn(definitionId: String, date: String) =
        settings.putString(KEY_LAST_CHECK_IN, key(definitionId, date))

    private fun key(definitionId: String, date: String) = "$date|$definitionId"

    private companion object {
        const val KEY_LAST_CHECK_IN = "attendance_last_check_in"
    }
}
