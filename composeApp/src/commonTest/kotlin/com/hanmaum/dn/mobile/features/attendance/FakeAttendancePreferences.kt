package com.hanmaum.dn.mobile.features.attendance

import com.hanmaum.dn.mobile.core.domain.repository.AttendancePreferences
import com.hanmaum.dn.mobile.core.domain.repository.RecordedAttendanceCheckIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAttendancePreferences : AttendancePreferences {
    private val _lastCheckIn = MutableStateFlow<RecordedAttendanceCheckIn?>(null)
    override val lastCheckIn: StateFlow<RecordedAttendanceCheckIn?> = _lastCheckIn

    override fun isCheckedIn(definitionId: String, date: String): Boolean =
        lastCheckIn.value == RecordedAttendanceCheckIn(definitionId, date)

    override fun markCheckedIn(definitionId: String, date: String) {
        _lastCheckIn.value = RecordedAttendanceCheckIn(definitionId, date)
    }

    override fun clearCheckedIn(definitionId: String, date: String) {
        if (isCheckedIn(definitionId, date)) _lastCheckIn.value = null
    }
}
