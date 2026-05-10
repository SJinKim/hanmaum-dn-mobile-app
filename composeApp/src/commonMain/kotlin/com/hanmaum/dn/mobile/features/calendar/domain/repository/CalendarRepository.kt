package com.hanmaum.dn.mobile.features.calendar.domain.repository

import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent

interface CalendarRepository {
    suspend fun getEvents(year: Int, month: Int): Result<List<CalendarEvent>>
    suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>>
}
