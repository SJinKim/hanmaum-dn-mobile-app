package com.hanmaum.dn.mobile.features.training.data.model

import kotlinx.serialization.Serializable

/*
 * Wire shapes of hanmaum-dn-server's TrainingDto, TrainingDetailDto, TrainingCourseDto and
 * MyTrainingApplicationDto (hanmaum-dn-server#167).
 *
 * The booleans the server always sends have no default on purpose. With ignoreUnknownKeys a
 * renamed key is silently dropped, and a default would then fill the gap with a plausible
 * value; that is how a deactivated 사역 once arrived as active (MinistryRepositoryImplTest).
 * Without a default a renamed key fails the call loudly instead.
 *
 * Timestamps stay strings here and are parsed in the repository, so one unreadable value
 * cannot fail a whole list.
 */

@Serializable
data class TrainingResponse(
    val publicId: String,
    val name: String,
    val description: String? = null,
    val openForRegistration: Boolean,
    val registrationStartsAt: String? = null,
    val registrationEndsAt: String? = null,
    val isAlwaysOpen: Boolean,
    val myApplication: MyTrainingApplicationResponse? = null,
)

@Serializable
data class TrainingDetailResponse(
    val publicId: String,
    val name: String,
    val description: String? = null,
    val startDate: String? = null,
    val durationWeeks: Int? = null,
    val openForRegistration: Boolean,
    /** java.time.DayOfWeek name, e.g. "SUNDAY". */
    val weekday: String? = null,
    /** "HH:mm". */
    val startTime: String? = null,
    val durationMinutes: Int? = null,
    val location: String? = null,
    val leaderName: String? = null,
    val targetAudience: List<String> = emptyList(),
    val registrationStartsAt: String? = null,
    val registrationEndsAt: String? = null,
    val isAlwaysOpen: Boolean,
    val courses: List<TrainingCourseResponse> = emptyList(),
    val myApplication: MyTrainingApplicationResponse? = null,
)

@Serializable
data class TrainingCourseResponse(
    val externalCourseId: Int,
    val name: String,
    val dateText: String? = null,
    val registrationStartsAt: String? = null,
    val registrationEndsAt: String? = null,
    val isAlwaysOpen: Boolean,
    val isEligible: Boolean,
)

@Serializable
data class MyTrainingApplicationResponse(
    val trainingPublicId: String,
    val externalCourseId: Int,
    val courseName: String,
    val appliedAt: String,
    val status: String,
)
