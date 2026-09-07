package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.RecordedAttendanceCheckIn
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttendancePreferencesImplTest {

    @Test
    fun markCheckedIn_updatesSharedStateAndPersistsIt() {
        val settings = MapSettings()
        val preferences = AttendancePreferencesImpl(settings)

        preferences.markCheckedIn("def-1", "2026-09-07")

        assertEquals(
            RecordedAttendanceCheckIn("def-1", "2026-09-07"),
            preferences.lastCheckIn.value,
        )
        assertTrue(preferences.isCheckedIn("def-1", "2026-09-07"))
        assertEquals(
            RecordedAttendanceCheckIn("def-1", "2026-09-07"),
            AttendancePreferencesImpl(settings).lastCheckIn.value,
        )
    }

    @Test
    fun aDifferentDefinitionOrDateDoesNotMatch() {
        val preferences = AttendancePreferencesImpl(MapSettings())
        preferences.markCheckedIn("def-1", "2026-09-07")

        assertFalse(preferences.isCheckedIn("def-2", "2026-09-07"))
        assertFalse(preferences.isCheckedIn("def-1", "2026-09-08"))
    }

    @Test
    fun clearCheckedIn_removesOnlyTheMatchingRecord() {
        val settings = MapSettings()
        val preferences = AttendancePreferencesImpl(settings)
        preferences.markCheckedIn("def-1", "2026-09-07")

        preferences.clearCheckedIn("def-2", "2026-09-07")
        assertTrue(preferences.isCheckedIn("def-1", "2026-09-07"))

        preferences.clearCheckedIn("def-1", "2026-09-07")
        assertNull(preferences.lastCheckIn.value)
        assertNull(AttendancePreferencesImpl(settings).lastCheckIn.value)
    }

    @Test
    fun malformedPersistedValueIsIgnored() {
        val preferences = AttendancePreferencesImpl(
            MapSettings("attendance_last_check_in" to "not-a-record"),
        )

        assertNull(preferences.lastCheckIn.value)
    }
}
