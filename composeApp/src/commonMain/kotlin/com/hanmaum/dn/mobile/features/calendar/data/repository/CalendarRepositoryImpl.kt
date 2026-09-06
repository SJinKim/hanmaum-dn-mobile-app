package com.hanmaum.dn.mobile.features.calendar.data.repository

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.features.calendar.data.model.GoogleCalendarEventItem
import com.hanmaum.dn.mobile.features.calendar.data.model.GoogleCalendarEventsResponse
import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent
import com.hanmaum.dn.mobile.features.calendar.domain.repository.CalendarRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

private const val GCAL_HOST = "https://www.googleapis.com"

/** Hard stop against a server that keeps handing out page tokens. */
private const val MAX_PAGES = 20

/** Google caps this at 2500; 250 keeps a normal month or year to one round trip. */
private const val PAGE_SIZE = 250

/**
 * Read-only view of the church's public Google calendars.
 *
 * Four things here are load-bearing and easy to break:
 *
 * 1. **The window is computed in Europe/Berlin, not UTC.** A `timeMin` of
 *    `2026-05-01T00:00:00Z` is 02:00 local in summer, so an event at 00:30 on
 *    the 1st falls outside the month it belongs to. `atStartOfDayIn` resolves
 *    the real instant and follows the DST switch on its own.
 * 2. **Every page is fetched.** `nextPageToken` is followed until Google stops
 *    handing one out, so "the month" means the whole month.
 * 3. **Every configured calendar is fetched**, in parallel, and the results are
 *    merged into one time-ordered list. The church keeps more than one calendar
 *    (the youth schedule is its own), and a screen that shows one of them while
 *    looking complete is the exact failure #178 was filed for.
 * 4. **One calendar failing fails the whole load.** Dropping the calendar that
 *    errored and rendering the rest would repeat that same failure quietly: a
 *    calendar accidentally set back to private would simply stop appearing.
 *    Better a visible error with a retry — and the view model keeps the events
 *    already on screen while it shows one.
 *
 * The calendar ids and key are constructor parameters (defaulted from
 * `BuildKonfig`) purely so tests can drive them; nothing else passes them.
 */
class CalendarRepositoryImpl(
    private val client: HttpClient,
    calendarIds: String = BuildKonfig.GOOGLE_CALENDAR_ID,
    private val apiKey: String = BuildKonfig.GOOGLE_CALENDAR_API_KEY,
    private val zone: TimeZone = TimeZone.of("Europe/Berlin"),
) : CalendarRepository {

    /**
     * `GOOGLE_CALENDAR_ID` holds one id or a comma-separated list of them. The
     * name stayed singular on purpose: renaming it would mean a new GitHub
     * secret in four workflow steps, and a forgotten one ships a calendar that
     * cannot load. A single id parses as a one-element list, so every existing
     * `.env` and secret keeps working untouched.
     */
    private val calendarIds: List<String> =
        calendarIds.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    override suspend fun getEvents(year: Int, month: Int): Result<List<CalendarEvent>> = runCatching {
        val from = LocalDate(year, month, 1)
        val to = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        fetchWindow(from, to)
    }

    override suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>> = runCatching {
        fetchWindow(LocalDate(year, 1, 1), LocalDate(year + 1, 1, 1))
    }

    /** `[from, to)` in church-local time, every calendar, every page, sorted by start. */
    private suspend fun fetchWindow(from: LocalDate, to: LocalDate): List<CalendarEvent> {
        requireConfigured()

        val timeMin = from.atStartOfDayIn(zone).toString()
        val timeMax = to.atStartOfDayIn(zone).toString()

        val perCalendar = coroutineScope {
            calendarIds
                .map { id -> async { fetchCalendar(id, timeMin, timeMax) } }
                .awaitAll()
        }

        return perCalendar
            .flatten()
            .sortedBy { it.startDate }
    }

    /** Every page of one calendar's window. */
    private suspend fun fetchCalendar(
        calendarId: String,
        timeMin: String,
        timeMax: String,
    ): List<CalendarEvent> {
        val items = mutableListOf<GoogleCalendarEventItem>()
        var pageToken: String? = null
        var page = 0

        do {
            val response = client.get(GCAL_HOST) {
                url {
                    appendPathSegments("calendar", "v3", "calendars", calendarId, "events")
                    parameters.append("key", apiKey)
                    parameters.append("timeMin", timeMin)
                    parameters.append("timeMax", timeMax)
                    parameters.append("timeZone", zone.id)
                    // Expands recurring events into the individual instances that
                    // land in the window; orderBy=startTime is only legal with it.
                    parameters.append("singleEvents", "true")
                    parameters.append("orderBy", "startTime")
                    parameters.append("maxResults", PAGE_SIZE.toString())
                    pageToken?.let { parameters.append("pageToken", it) }
                }
            }

            // The shared client runs with expectSuccess = false, so a 403 arrives
            // as a normal response whose body is an error envelope. Name the
            // calendar and the status and nothing else — the query string carries
            // the API key. A 404 here almost always means that calendar is not
            // public, which no amount of client code can fix.
            if (!response.status.isSuccess()) {
                throw CalendarApiException(
                    "Google Calendar request failed for $calendarId: ${response.status.value}",
                )
            }

            val body = response.body<GoogleCalendarEventsResponse>()
            items += body.items
            pageToken = body.nextPageToken
            page++
        } while (pageToken != null && page < MAX_PAGES)

        return items
            .filter { it.status != "cancelled" }
            .map { it.toDomain(calendarId) }
    }

    /**
     * A blank id or key would otherwise go out as a real request and come back
     * as an opaque 400 — the five-place config rule (CLAUDE.md §4) makes an
     * empty value the likeliest failure, so it gets named as one.
     */
    private fun requireConfigured() {
        if (calendarIds.isEmpty()) {
            throw CalendarApiException("GOOGLE_CALENDAR_ID is not configured")
        }
        if (apiKey.isBlank()) {
            throw CalendarApiException("GOOGLE_CALENDAR_API_KEY is not configured")
        }
    }

    private fun GoogleCalendarEventItem.toDomain(calendarId: String) = CalendarEvent(
        id          = id,
        calendarId  = calendarId,
        title       = summary,
        description = description,
        location    = location,
        startDate   = start.date ?: start.dateTime ?: "",
        endDate     = end.date   ?: end.dateTime   ?: "",
        isAllDay    = start.date != null,
    )
}

/** Carries a diagnosable reason without ever carrying the API key. */
class CalendarApiException(message: String) : Exception(message)
