package com.hanmaum.dn.mobile.features.training

import com.hanmaum.dn.mobile.features.training.domain.model.RegistrationWindow
import com.hanmaum.dn.mobile.features.training.domain.model.Training
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplication
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplicationStatus
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingCourse
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingDetail
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import com.hanmaum.dn.mobile.features.training.domain.repository.TrainingRepository
import kotlin.time.Instant

/** Hand-written, like every other fake here — there is no mocking library. */
class FakeTrainingRepository : TrainingRepository {

    var listResult: TrainingResult<List<Training>> = TrainingResult.Success(emptyList())
    var detailResult: TrainingResult<TrainingDetail> = TrainingResult.Success(detail())

    var listCalls = 0
    var lastDetailId: String? = null

    override suspend fun getTrainings(): TrainingResult<List<Training>> {
        listCalls++
        return listResult
    }

    override suspend fun getTrainingDetail(publicId: String): TrainingResult<TrainingDetail> {
        lastDetailId = publicId
        return detailResult
    }

    companion object {
        val NO_WINDOW = RegistrationWindow(startsAt = null, endsAt = null, isAlwaysOpen = false)

        fun training(id: String = "t1", name: String = "큐티베이직세미나", open: Boolean = true) = Training(
            publicId = id,
            name = name,
            description = "매일 말씀 묵상을 시작하는 기초 과정",
            openForRegistration = open,
            window = NO_WINDOW,
            myApplication = null,
        )

        fun course(id: Int, eligible: Boolean = true) = TrainingCourse(
            externalCourseId = id,
            name = "큐베세 $id 반",
            dateText = null,
            window = NO_WINDOW,
            isEligible = eligible,
        )

        fun application(status: TrainingApplicationStatus) = TrainingApplication(
            externalCourseId = 106,
            courseName = "큐베세 직장인/청년 반",
            appliedAt = Instant.parse("2026-09-14T10:00:00Z"),
            status = status,
        )

        fun detail(
            courses: List<TrainingCourse> = emptyList(),
            application: TrainingApplication? = null,
        ) = TrainingDetail(
            publicId = "t1",
            name = "큐티베이직세미나",
            description = null,
            startDate = null,
            durationWeeks = null,
            weekday = null,
            startTime = null,
            durationMinutes = null,
            location = null,
            leaderName = null,
            targetAudience = emptyList(),
            openForRegistration = courses.any { it.isEligible },
            window = NO_WINDOW,
            courses = courses,
            myApplication = application,
        )
    }
}
