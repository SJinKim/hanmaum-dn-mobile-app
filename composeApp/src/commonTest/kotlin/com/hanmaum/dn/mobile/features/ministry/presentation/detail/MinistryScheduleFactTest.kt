package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import com.hanmaum.dn.mobile.core.i18n.KoStrings
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistrySchedule
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinistryScheduleFactTest {

    private fun schedule(
        description: String? = null,
        day: DayOfWeek? = null,
        start: String? = null,
        end: String? = null,
        location: String? = null,
    ) = MinistrySchedule(description, day, start, end, location).facts(KoStrings).map { it.label to it.value }

    @Test
    fun weekdayAndTimeRangeFormTheMeetingTimeRowAndPlaceGetsItsOwn() {
        assertEquals(
            listOf("모임 시간" to "매주 토요일 16:00–18:00", "장소" to "본당"),
            schedule(day = DayOfWeek.SATURDAY, start = "16:00:00", end = "18:00:00", location = "본당"),
        )
    }

    @Test
    fun onlyThePlaceRowIsMarkedAsPlace() {
        val facts = MinistrySchedule(null, DayOfWeek.MONDAY, "19:00", null, "본당").facts(KoStrings)
        assertEquals(listOf(false, true), facts.map { it.isPlace })
    }

    @Test
    fun sundayMapsToTheFirstDayHeader() {
        assertEquals(listOf("모임 시간" to "매주 일요일"), schedule(day = DayOfWeek.SUNDAY))
    }

    @Test
    fun descriptionLabelsTheTimeRow() {
        assertEquals(listOf("주일 연습" to "07:00"), schedule("주일 연습", start = "07:00"))
    }

    @Test
    fun descriptionAloneBecomesTheValue() {
        assertEquals(listOf("모임 시간" to "매월 첫째 주"), schedule("매월 첫째 주"))
    }

    @Test
    fun blankScheduleHasNoRow() {
        assertTrue(schedule(" ", null, "", null, " ").isEmpty())
    }
}
