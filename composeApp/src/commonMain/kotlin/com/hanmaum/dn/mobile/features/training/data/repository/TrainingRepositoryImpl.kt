package com.hanmaum.dn.mobile.features.training.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiErrorResponse
import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.training.data.model.MyTrainingApplicationResponse
import com.hanmaum.dn.mobile.features.training.data.model.TrainingCourseResponse
import com.hanmaum.dn.mobile.features.training.data.model.TrainingDetailResponse
import com.hanmaum.dn.mobile.features.training.data.model.TrainingResponse
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
import io.ktor.client.statement.HttpResponse
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
            response.errorCode() == CODE_UNAVAILABLE -> TrainingResult.Unavailable
            else -> TrainingResult.Failed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TrainingResult.Failed
    }

    private suspend fun HttpResponse.errorCode(): String? =
        runCatching { body<ApiErrorResponse>().code }.getOrNull()

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private fun TrainingResponse.toDomain() = Training(
        publicId = publicId,
        name = name,
        description = description?.takeIf { it.isNotBlank() },
        openForRegistration = openForRegistration,
        window = window(registrationStartsAt, registrationEndsAt, isAlwaysOpen),
        myApplication = myApplication?.toDomain(),
    )

    private fun TrainingDetailResponse.toDomain() = TrainingDetail(
        publicId = publicId,
        name = name,
        description = description?.takeIf { it.isNotBlank() },
        startDate = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
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
    )

    private fun TrainingCourseResponse.toDomain() = TrainingCourse(
        externalCourseId = externalCourseId,
        name = name,
        dateText = dateText?.takeIf { it.isNotBlank() },
        window = window(registrationStartsAt, registrationEndsAt, isAlwaysOpen),
        isEligible = isEligible,
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

    private companion object {
        const val CODE_UNAVAILABLE = "COURSE_APPLICATION_UNAVAILABLE"
    }
}
