package com.hanmaum.dn.mobile.features.training.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiErrorResponse
import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.training.data.model.ApplicantPrefillResponse
import com.hanmaum.dn.mobile.features.training.data.model.CourseFormFieldResponse
import com.hanmaum.dn.mobile.features.training.data.model.MyTrainingApplicationResponse
import com.hanmaum.dn.mobile.features.training.data.model.TrainingApplicationRequest
import com.hanmaum.dn.mobile.features.training.data.model.TrainingCourseResponse
import com.hanmaum.dn.mobile.features.training.data.model.TrainingDetailResponse
import com.hanmaum.dn.mobile.features.training.data.model.TrainingRegistrationResponse
import com.hanmaum.dn.mobile.features.training.data.model.TrainingResponse
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicantPrefill
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicationField
import com.hanmaum.dn.mobile.features.training.domain.model.ApplyResult
import com.hanmaum.dn.mobile.features.training.domain.model.CourseFormField
import com.hanmaum.dn.mobile.features.training.domain.model.FormFieldOption
import com.hanmaum.dn.mobile.features.training.domain.model.RegistrationWindow
import com.hanmaum.dn.mobile.features.training.domain.model.Training
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplication
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplicationStatus
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingCourse
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingDetail
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import com.hanmaum.dn.mobile.features.training.domain.repository.TrainingRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

class TrainingRepositoryImpl(
    private val client: HttpClient,
) : TrainingRepository {

    // activeOnly=true is the 양육 list. Without it the same endpoint answers with the stored
    // admin catalog: no external data, no member-specific flags, discontinued courses included.
    override suspend fun getTrainings(): TrainingResult<List<Training>> = call(
        request = { client.get("trainings?activeOnly=true") },
        read = { response ->
            response.body<ApiResponse<List<TrainingResponse>>>().data.orEmpty().map { it.toDomain() }
        },
    )

    override suspend fun getTrainingDetail(publicId: String): TrainingResult<TrainingDetail> = call(
        request = { client.get("trainings/$publicId") },
        read = { response -> response.body<ApiResponse<TrainingDetailResponse>>().data?.toDomain() },
    )

    override suspend fun apply(
        trainingPublicId: String,
        externalCourseId: Int,
        values: Map<ApplicationField, String>,
    ): ApplyResult = try {
        val response = client.post("trainings/$trainingPublicId/registrations") {
            contentType(ContentType.Application.Json)
            setBody(values.toRequest(externalCourseId))
        }
        if (response.status.isSuccess()) {
            response.body<ApiResponse<TrainingRegistrationResponse>>().data
                ?.let { ApplyResult.Success(it.courseName) }
                ?: ApplyResult.Failed
        } else {
            applyFailure(response.status.value, response.errorBody())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApplyResult.Failed
    }

    /**
     * The client has expectSuccess = false, so non-2xx comes back normally and is mapped here.
     *
     * A 503 means 준비중 only when the server says so by code. A proxy answering 503 while the
     * server restarts is an outage and must offer a retry, not claim the feature is coming.
     */
    private suspend fun <T> call(
        request: suspend () -> HttpResponse,
        read: suspend (HttpResponse) -> T?,
    ): TrainingResult<T> = try {
        val response = request()
        when {
            response.status.isSuccess() -> read(response)?.let { TrainingResult.Success(it) } ?: TrainingResult.Failed
            response.errorBody()?.code == CODE_UNAVAILABLE -> TrainingResult.Unavailable
            else -> TrainingResult.Failed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TrainingResult.Failed
    }

    private suspend fun HttpResponse.errorBody(): ApiErrorResponse? =
        runCatching { body<ApiErrorResponse>() }.getOrNull()

    /** One outcome per code (hanmaum-dn-server's ApiErrorCode); the HTTP status alone decides nothing. */
    private fun applyFailure(status: Int, error: ApiErrorResponse?): ApplyResult = when (error?.code) {
        CODE_UNAVAILABLE -> ApplyResult.Unavailable
        "COURSE_APPLICATION_FULL" -> ApplyResult.Full
        "COURSE_APPLICATION_CLOSED" -> ApplyResult.Closed
        "COURSE_APPLICATION_ALREADY_APPLIED" -> ApplyResult.AlreadyApplied
        "COURSE_APPLICATION_NOT_ELIGIBLE", "COURSE_APPLICATION_COURSE_NOT_FOUND" ->
            ApplyResult.NotEligible(error.message?.takeIf { it.isNotBlank() })
        "COURSE_APPLICATION_INVALID" -> {
            val fieldErrors = error.fieldErrors.orEmpty()
            ApplyResult.Invalid(
                fieldErrors = fieldErrors.mapNotNull { (name, message) ->
                    ApplicationField.fromWire(name)?.let { it to message }
                }.toMap(),
                // With field errors the message is only "입력값을 확인해주세요."; without them it
                // is the one thing that says what went wrong.
                message = error.message?.takeIf { fieldErrors.isEmpty() && it.isNotBlank() },
            )
        }
        // Request validation (e.g. a malformed email) answers 400 without a code and with a
        // technical message in English, which is not for members.
        null -> if (status == 400) ApplyResult.Invalid(emptyMap(), message = null) else ApplyResult.Failed
        else -> ApplyResult.Failed
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private fun Map<ApplicationField, String>.toRequest(externalCourseId: Int): TrainingApplicationRequest {
        fun valueOf(field: ApplicationField) = this[field]?.trim()?.takeIf { it.isNotEmpty() }
        return TrainingApplicationRequest(
            externalCourseId = externalCourseId,
            name = valueOf(ApplicationField.NAME),
            birthDate = valueOf(ApplicationField.BIRTH_DATE),
            email = valueOf(ApplicationField.EMAIL),
            phone = valueOf(ApplicationField.PHONE),
            gender = valueOf(ApplicationField.GENDER),
            baptized = valueOf(ApplicationField.BAPTIZED),
            baptizeType = valueOf(ApplicationField.BAPTIZE_TYPE),
            residence = valueOf(ApplicationField.RESIDENCE),
            gyogu = valueOf(ApplicationField.GYOGU),
            soon = valueOf(ApplicationField.SOON),
            children = valueOf(ApplicationField.CHILDREN),
            history = valueOf(ApplicationField.HISTORY),
            waiting = valueOf(ApplicationField.WAITING),
            running = valueOf(ApplicationField.RUNNING),
            comment = valueOf(ApplicationField.COMMENT),
        )
    }

    private fun TrainingResponse.toDomain() = Training(
        publicId = publicId,
        // The catalog name is English for the seeded trainings; members know the Korean one.
        name = nameKo?.takeIf { it.isNotBlank() } ?: name,
        description = description?.takeIf { it.isNotBlank() },
        openForRegistration = openForRegistration,
        window = window(registrationStartsAt, registrationEndsAt, isAlwaysOpen),
        myApplication = myApplication?.toDomain(),
    )

    private fun TrainingDetailResponse.toDomain() = TrainingDetail(
        publicId = publicId,
        // The catalog name is English for the seeded trainings; members know the Korean one.
        name = nameKo?.takeIf { it.isNotBlank() } ?: name,
        description = description?.takeIf { it.isNotBlank() },
        startDate = startDate?.toLocalDateOrNull(),
        durationWeeks = durationWeeks,
        weekday = weekday?.let { wire -> DayOfWeek.entries.firstOrNull { it.name == wire } },
        startTime = startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        durationMinutes = durationMinutes,
        location = location?.takeIf { it.isNotBlank() },
        leaderName = leaderName?.takeIf { it.isNotBlank() },
        targetAudience = targetAudience.filter { it.isNotBlank() },
        openForRegistration = openForRegistration,
        window = window(registrationStartsAt, registrationEndsAt, isAlwaysOpen),
        courses = courses.map { it.toDomain() },
        myApplication = myApplication?.toDomain(),
        applicantPrefill = applicantPrefill?.toDomain(),
    )

    private fun TrainingCourseResponse.toDomain() = TrainingCourse(
        externalCourseId = externalCourseId,
        name = name,
        dateText = dateText?.takeIf { it.isNotBlank() },
        window = window(registrationStartsAt, registrationEndsAt, isAlwaysOpen),
        isEligible = isEligible,
        formFields = formFields.map { it.toDomain() },
    )

    private fun CourseFormFieldResponse.toDomain() = CourseFormField(
        name = name,
        type = type,
        required = required,
        label = label?.takeIf { it.isNotBlank() },
        options = options.map { FormFieldOption(value = it.value, label = it.label?.takeIf { l -> l.isNotBlank() }) },
    )

    private fun ApplicantPrefillResponse.toDomain() = ApplicantPrefill(
        name = name?.takeIf { it.isNotBlank() },
        birthDate = birthDate?.toLocalDateOrNull(),
        email = email?.takeIf { it.isNotBlank() },
        phone = phone?.takeIf { it.isNotBlank() },
        gender = gender?.takeIf { it.isNotBlank() },
        residence = residence?.takeIf { it.isNotBlank() },
        history = history?.takeIf { it.isNotBlank() },
        waiting = waiting?.takeIf { it.isNotBlank() },
        running = running?.takeIf { it.isNotBlank() },
        baptized = baptized?.takeIf { it.isNotBlank() },
        baptizeType = baptizeType?.takeIf { it.isNotBlank() },
    )

    /**
     * An unreadable appliedAt keeps the application and drops only the date. Dropping the
     * whole application would offer 신청하기 to a member who has already applied.
     */
    private fun MyTrainingApplicationResponse.toDomain() = TrainingApplication(
        externalCourseId = externalCourseId,
        courseName = courseName,
        appliedAt = appliedAt.toInstantOrNull(),
        status = TrainingApplicationStatus.fromWire(status),
    )

    private fun window(startsAt: String?, endsAt: String?, isAlwaysOpen: Boolean) = RegistrationWindow(
        startsAt = startsAt?.toInstantOrNull(),
        endsAt = endsAt?.toInstantOrNull(),
        isAlwaysOpen = isAlwaysOpen,
    )

    // The server sends OffsetDateTime ("2026-09-21T23:59:59+02:00") and Instant ("…Z").
    private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

    private companion object {
        const val CODE_UNAVAILABLE = "COURSE_APPLICATION_UNAVAILABLE"
    }
}
