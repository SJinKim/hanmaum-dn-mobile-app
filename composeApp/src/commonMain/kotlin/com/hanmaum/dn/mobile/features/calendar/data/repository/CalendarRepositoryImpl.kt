package com.hanmaum.dn.mobile.features.calendar.data.repository

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.features.calendar.data.model.GoogleCalendarEventItem
import com.hanmaum.dn.mobile.features.calendar.data.model.GoogleCalendarEventsResponse
import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent
import com.hanmaum.dn.mobile.features.calendar.domain.repository.CalendarRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val GCAL_BASE = "https://www.googleapis.com/calendar/v3/calendars"

class CalendarRepositoryImpl(private val client: HttpClient) : CalendarRepository {

    private val calendarId = BuildKonfig.GOOGLE_CALENDAR_ID.replace("@", "%40")
    private val apiKey     = BuildKonfig.GOOGLE_CALENDAR_API_KEY

    override suspend fun getEvents(year: Int, month: Int): Result<List<CalendarEvent>> = runCatching {
        val mm  = month.toString().padStart(2, '0')
        val nm  = if (month == 12) "${year + 1}-01" else "${year}-${(month + 1).toString().padStart(2, '0')}"
        val url = "$GCAL_BASE/$calendarId/events" +
            "?key=$apiKey" +
            "&timeMin=${year}-${mm}-01T00:00:00Z" +
            "&timeMax=${nm}-01T00:00:00Z" +
            "&orderBy=startTime&singleEvents=true&maxResults=100"

        val body = client.get(url).body<GoogleCalendarEventsResponse>()
        body.items.map { it.toDomain() }
    }

    override suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>> = runCatching {
        val url = "$GCAL_BASE/$calendarId/events" +
            "?key=$apiKey" +
            "&timeMin=${year}-01-01T00:00:00Z" +
            "&timeMax=${year + 1}-01-01T00:00:00Z" +
            "&orderBy=startTime&singleEvents=true&maxResults=500"
        val body = client.get(url).body<GoogleCalendarEventsResponse>()
        body.items.map { it.toDomain() }
    }

    private fun GoogleCalendarEventItem.toDomain() = CalendarEvent(
        id          = id,
        title       = summary,
        description = description,
        location    = location,
        startDate   = start.date ?: start.dateTime ?: "",
        endDate     = end.date   ?: end.dateTime   ?: "",
        isAllDay    = start.date != null,
    )
}
