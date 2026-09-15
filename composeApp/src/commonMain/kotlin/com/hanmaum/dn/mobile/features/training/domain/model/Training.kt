package com.hanmaum.dn.mobile.features.training.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/**
 * One 양육 training as the list shows it.
 *
 * Every flag here is computed by the server for the signed-in member, including
 * [openForRegistration] (a 여자반 open only to women reads as closed for everyone else)
 * and the position in the list. The app draws it and derives nothing.
 */
data class Training(
    val publicId: String,
    val name: String,
    val description: String?,
    val openForRegistration: Boolean,
    val window: RegistrationWindow,
    val myApplication: TrainingApplication?,
)

/**
 * When applications are taken. A null bound is open on that side.
 *
 * [isAlwaysOpen] is 상시 접수: the server sets it when the end is so far away that it is not
 * a real deadline, and the app shows that wording instead of a date.
 */
data class RegistrationWindow(
    val startsAt: Instant?,
    val endsAt: Instant?,
    val isAlwaysOpen: Boolean,
)

data class TrainingDetail(
    val publicId: String,
    val name: String,
    val description: String?,
    val startDate: LocalDate?,
    val durationWeeks: Int?,
    val weekday: DayOfWeek?,
    val startTime: LocalTime?,
    val durationMinutes: Int?,
    val location: String?,
    val leaderName: String?,
    val targetAudience: List<String>,
    val openForRegistration: Boolean,
    val window: RegistrationWindow,
    /** Courses open right now, including ones this member may not apply to. */
    val courses: List<TrainingCourse>,
    val myApplication: TrainingApplication?,
)

/** One 반 of a training in application.hanmaum.de, e.g. 큐베세 직장인/청년 반. */
data class TrainingCourse(
    val externalCourseId: Int,
    val name: String,
    /** Free-text schedule as published, e.g. "3월 2일 - 3월 23일 매주 월요일 저녁 7시30분 비전홀". */
    val dateText: String?,
    val window: RegistrationWindow,
    /** False when this member may not apply. The server gives no reason. */
    val isEligible: Boolean,
)

/** An application the member made through the app: 신청 현황. */
data class TrainingApplication(
    val externalCourseId: Int,
    val courseName: String,
    /** Null only when the server sent a timestamp this client cannot read. */
    val appliedAt: Instant?,
    val status: TrainingApplicationStatus,
)

enum class TrainingApplicationStatus {
    APPLIED,
    ENROLLED,
    IN_PROGRESS,
    COMPLETED,
    DROPPED,

    /** Sent by the server for a status it cannot name, and used here for any value this app does not know. */
    UNKNOWN,
    ;

    /**
     * Still running. The server refuses a second application with ALREADY_APPLIED for exactly
     * these, so the app does not offer 신청하기 and does offer 신청 취소.
     */
    val isActive: Boolean
        get() = this == APPLIED || this == ENROLLED || this == IN_PROGRESS

    companion object {
        fun fromWire(value: String): TrainingApplicationStatus =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}
