package com.hanmaum.dn.mobile.features.training.presentation

import com.hanmaum.dn.mobile.core.i18n.KoStrings
import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository
import com.hanmaum.dn.mobile.features.training.domain.model.RegistrationWindow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class TrainingFormatTest {

    private val berlin = TimeZone.of("Europe/Berlin")

    private fun window(start: String?, end: String?, alwaysOpen: Boolean = false) = RegistrationWindow(
        startsAt = start?.let { Instant.parse(it) },
        endsAt = end?.let { Instant.parse(it) },
        isAlwaysOpen = alwaysOpen,
    )

    @Test
    fun alwaysOpenIsSaidInsteadOfADate() {
        val text = TrainingFormat.window(KoStrings, window(null, "2099-03-12T23:59:59+01:00", alwaysOpen = true), berlin)

        assertEquals("상시 접수", text)
    }

    @Test
    fun bothBoundsFormTheWindow() {
        val text = TrainingFormat.window(
            KoStrings,
            window("2026-09-01T00:00:00+02:00", "2026-09-21T23:59:59+02:00"),
            berlin,
        )

        assertEquals("신청 9월 1일 – 9월 21일", text)
    }

    @Test
    fun anOpenStartShowsOnlyTheEnd() {
        assertEquals(
            "신청 ~ 9월 21일",
            TrainingFormat.window(KoStrings, window(null, "2026-09-21T23:59:59+02:00"), berlin),
        )
    }

    @Test
    fun noBoundsSayNothing() {
        assertNull(TrainingFormat.window(KoStrings, window(null, null), berlin))
    }

    @Test
    fun datesFollowTheGivenZone() {
        // 23:30 UTC is already the next day in Berlin.
        assertEquals(
            "신청 ~ 9월 22일",
            TrainingFormat.window(KoStrings, window(null, "2026-09-21T23:30:00Z"), berlin),
        )
    }

    @Test
    fun theScheduleJoinsWeekdayTimeAndLength() {
        val detail = FakeTrainingRepository.detail().copy(
            weekday = DayOfWeek.SUNDAY,
            startTime = LocalTime(14, 0),
            durationMinutes = 60,
        )

        assertEquals("매주 일요일 · 14:00 · 60분", TrainingFormat.schedule(KoStrings, detail))
    }

    @Test
    fun thePeriodJoinsStartAndWeeks() {
        val detail = FakeTrainingRepository.detail().copy(startDate = LocalDate(2026, 9, 7), durationWeeks = 4)

        assertEquals("9월 7일 시작 · 4주", TrainingFormat.period(KoStrings, detail))
    }

    @Test
    fun anEmptyScheduleIsNull() {
        assertNull(TrainingFormat.schedule(KoStrings, FakeTrainingRepository.detail()))
    }

    @Test
    fun theApplicationDateIsTheLocalDay() {
        assertEquals(
            "2026년 9월 15일",
            TrainingFormat.appliedOn(KoStrings, Instant.parse("2026-09-14T22:30:00Z"), berlin),
        )
    }
}
