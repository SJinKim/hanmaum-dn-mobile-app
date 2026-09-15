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
    /** The catalog name, English for the seeded trainings (e.g. "Quiet Time Basic Seminar"). */
    val name: String,
    /** The Korean name members know (e.g. "큐티베이직세미나"). */
    val nameKo: String? = null,
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
    val applicantPrefill: ApplicantPrefillResponse? = null,
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
    val formFields: List<CourseFormFieldResponse> = emptyList(),
)

@Serializable
data class CourseFormFieldResponse(
    val name: String,
    val type: String? = null,
    val required: Boolean,
    val label: String? = null,
    val options: List<CourseFormFieldOptionResponse> = emptyList(),
)

@Serializable
data class CourseFormFieldOptionResponse(
    val value: String,
    val label: String? = null,
)

@Serializable
data class ApplicantPrefillResponse(
    val name: String? = null,
    /** "YYYY-MM-DD". */
    val birthDate: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val residence: String? = null,
)

/**
 * Body of POST /trainings/{publicId}/registrations. A null field is not encoded (it has a
 * default), which is what lets the server fall back to the member's profile for it.
 */
@Serializable
data class TrainingApplicationRequest(
    val externalCourseId: Int,
    val name: String? = null,
    /** "YYYY-MM-DD". */
    val birthDate: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val baptized: String? = null,
    val baptizeType: String? = null,
    val residence: String? = null,
    val gyogu: String? = null,
    val soon: String? = null,
    val children: String? = null,
    val history: String? = null,
    val waiting: String? = null,
    val running: String? = null,
    val comment: String? = null,
)

@Serializable
data class TrainingRegistrationResponse(
    val trainingPublicId: String,
    val status: String,
    val externalCourseId: Int,
    val courseName: String,
)

@Serializable
data class MyTrainingApplicationResponse(
    val trainingPublicId: String,
    val externalCourseId: Int,
    val courseName: String,
    val appliedAt: String,
    val status: String,
)
