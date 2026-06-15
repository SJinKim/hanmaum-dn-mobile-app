package com.hanmaum.dn.mobile.features.attendance

import com.hanmaum.dn.mobile.core.domain.repository.AttendancePreferences

class FakeAttendancePreferences : AttendancePreferences {
    private var stored: String? = null

    override fun isCheckedIn(definitionId: String, date: String): Boolean =
        stored == "$date|$definitionId"

    override fun markCheckedIn(definitionId: String, date: String) {
        stored = "$date|$definitionId"
    }
}
