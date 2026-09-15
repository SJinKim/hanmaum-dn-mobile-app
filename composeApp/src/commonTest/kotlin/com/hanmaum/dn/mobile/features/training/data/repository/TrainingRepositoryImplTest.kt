package com.hanmaum.dn.mobile.features.training.data.repository

import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplicationStatus
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private fun mockClient(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    contentType: ContentType = ContentType.Application.Json,
    onRequest: (HttpRequestData) -> Unit = {},
): HttpClient = HttpClient(MockEngine { request ->
    onRequest(request)
    respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, contentType.toString()))
}) {
    // Same settings as the app's client in NetworkClient.kt.
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
    defaultRequest {
        if (url.host.isBlank()) {
            val path = url.encodedPath.removePrefix("/")
            url.takeFrom("http://localhost")
            url.encodedPath = "/$path"
        }
    }
}

/** Response shapes copied from hanmaum-dn-server's TrainingControllerTest (hanmaum-dn-server#167). */
class TrainingRepositoryImplTest {

    private val listJson = """
        {"success":true,"message":null,"data":[
          {"publicId":"t-open","name":"일대일 제자양육","sortOrder":4,"description":"일대일로 함께 걷는 과정",
           "startDate":null,"durationWeeks":null,"openForRegistration":true,
           "registrationStartsAt":null,"registrationEndsAt":"2099-03-12T23:59:59+01:00","isAlwaysOpen":true,
           "myApplication":{"trainingPublicId":"t-open","trainingName":"ONE_ON_ONE","trainingNameKo":"일대일 제자양육",
             "externalCourseId":3,"courseName":"일대일 제자양육","appliedAt":"2026-09-14T10:00:00Z","status":"APPLIED"}},
          {"publicId":"t-closed","name":"큐티베이직세미나","sortOrder":1,"description":"",
           "startDate":"2026-09-07","durationWeeks":4,"openForRegistration":false,
           "registrationStartsAt":"2026-08-01T00:00:00+02:00","registrationEndsAt":"2026-08-31T23:59:59+02:00",
           "isAlwaysOpen":false,"myApplication":null}
        ]}
    """.trimIndent()

    @Test
    fun theListAsksForTheActiveOnlyView() = runTest {
        // Without activeOnly=true the endpoint serves the admin catalog: no member flags at all.
        var seen: HttpRequestData? = null
        TrainingRepositoryImpl(mockClient(listJson) { seen = it }).getTrainings()

        val request = assertNotNull(seen)
        assertEquals("/trainings", request.url.encodedPath)
        assertEquals("true", request.url.parameters["activeOnly"])
    }

    @Test
    fun theListKeepsTheServerOrderAndBindsEveryFlag() = runTest {
        val result = TrainingRepositoryImpl(mockClient(listJson)).getTrainings()

        val trainings = assertIs<TrainingResult.Success<*>>(result).data as List<*>
        val open = trainings[0] as com.hanmaum.dn.mobile.features.training.domain.model.Training
        val closed = trainings[1] as com.hanmaum.dn.mobile.features.training.domain.model.Training

        assertEquals(listOf("t-open", "t-closed"), listOf(open.publicId, closed.publicId))
        assertTrue(open.openForRegistration)
        assertTrue(open.window.isAlwaysOpen, "isAlwaysOpen must come from the wire key isAlwaysOpen")
        assertEquals(Instant.parse("2099-03-12T22:59:59Z"), open.window.endsAt)
        assertEquals(TrainingApplicationStatus.APPLIED, open.myApplication?.status)
        assertEquals(Instant.parse("2026-09-14T10:00:00Z"), open.myApplication?.appliedAt)

        assertFalse(closed.openForRegistration)
        assertNull(closed.description, "a blank description is no description")
        assertEquals(Instant.parse("2026-07-31T22:00:00Z"), closed.window.startsAt)
        assertNull(closed.myApplication)
    }

    @Test
    fun aRenamedFlagFailsInsteadOfFallingBackToADefault() = runTest {
        // The server once documented isActive as "active"; a default on the client hid it.
        val json = """{"success":true,"data":[{"publicId":"t1","name":"새가족반","open":true,"alwaysOpen":false}]}"""

        assertEquals(TrainingResult.Failed, TrainingRepositoryImpl(mockClient(json)).getTrainings())
    }

    @Test
    fun theUnavailableCodeIsNotAnError() = runTest {
        val json = """
            {"timestamp":"2026-09-15T12:00:00+02:00","status":503,"message":"준비중입니다.",
             "error":"Service Unavailable","code":"COURSE_APPLICATION_UNAVAILABLE","fieldErrors":null}
        """.trimIndent()
        val client = mockClient(json, status = HttpStatusCode.ServiceUnavailable)

        assertEquals(TrainingResult.Unavailable, TrainingRepositoryImpl(client).getTrainings())
        assertEquals(TrainingResult.Unavailable, TrainingRepositoryImpl(client).getTrainingDetail("t1"))
    }

    @Test
    fun a503WithoutTheCodeIsAnOutage() = runTest {
        // A proxy's 503 while the server restarts must offer a retry, not claim 준비중.
        val client = mockClient(
            "<html>503 Service Temporarily Unavailable</html>",
            status = HttpStatusCode.ServiceUnavailable,
            contentType = ContentType.Text.Html,
        )

        assertEquals(TrainingResult.Failed, TrainingRepositoryImpl(client).getTrainings())
    }

    @Test
    fun anotherErrorCodeIsAFailure() = runTest {
        val json = """{"status":404,"message":"No training with that id exists.","error":"Not Found","code":null}"""
        val client = mockClient(json, status = HttpStatusCode.NotFound)

        assertEquals(TrainingResult.Failed, TrainingRepositoryImpl(client).getTrainingDetail("missing"))
    }

    @Test
    fun theDetailBindsCoursesEligibilityAndSchedule() = runTest {
        val json = """
            {"success":true,"data":{
              "publicId":"t1","name":"큐티베이직세미나","nameKo":"큐티베이직세미나","category":null,"sortOrder":1,
              "description":"큐티를 처음 시작하는 과정","startDate":"2026-09-07","durationWeeks":4,
              "openForRegistration":true,"weekday":"SUNDAY","startTime":"14:00","durationMinutes":60,
              "location":"교육관 201호","leaderName":"이하늘 전도사","capacity":12,"registeredCount":8,
              "registrationDeadline":null,"targetAudience":["큐티를 처음 시작하는 분",""],
              "registrationStartsAt":"2026-09-01T00:00:00+02:00","registrationEndsAt":"2026-09-30T23:59:59+02:00",
              "isAlwaysOpen":false,
              "courses":[
                {"externalCourseId":105,"name":"큐베세 여자반","dateText":"3월 4일 - 3월 25일","description":null,
                 "secondaryText":null,"registrationStartsAt":null,"registrationEndsAt":"2026-09-30T23:59:59+02:00",
                 "isAlwaysOpen":false,"isEligible":false,"formFields":[]},
                {"externalCourseId":106,"name":"큐베세 직장인/청년 반","dateText":"","description":null,
                 "secondaryText":null,"registrationStartsAt":null,"registrationEndsAt":null,
                 "isAlwaysOpen":true,"isEligible":true,"formFields":[{"name":"phone","type":null,"required":true,"label":null,"options":[]}]}
              ],
              "myApplication":null,
              "applicantPrefill":{"name":"김철수","birthDate":"1995-03-14","email":null,"phone":null,"gender":"M","residence":null}
            }}
        """.trimIndent()

        val result = TrainingRepositoryImpl(mockClient(json)).getTrainingDetail("t1")

        val detail = assertIs<TrainingResult.Success<com.hanmaum.dn.mobile.features.training.domain.model.TrainingDetail>>(result).data
        assertEquals(DayOfWeek.SUNDAY, detail.weekday)
        assertEquals(LocalTime(14, 0), detail.startTime)
        assertEquals(listOf("큐티를 처음 시작하는 분"), detail.targetAudience)
        assertEquals(listOf(105, 106), detail.courses.map { it.externalCourseId })
        assertFalse(detail.courses[0].isEligible, "isEligible must come from the wire key isEligible")
        assertTrue(detail.courses[1].isEligible)
        assertTrue(detail.courses[1].window.isAlwaysOpen)
        assertNull(detail.courses[1].dateText)
    }

    @Test
    fun anUnknownStatusIsKeptAsUnknown() = runTest {
        val json = listJson.replace("\"status\":\"APPLIED\"", "\"status\":\"WAITLISTED\"")

        val result = TrainingRepositoryImpl(mockClient(json)).getTrainings()

        val first = (assertIs<TrainingResult.Success<*>>(result).data as List<*>).first()
            as com.hanmaum.dn.mobile.features.training.domain.model.Training
        assertEquals(TrainingApplicationStatus.UNKNOWN, first.myApplication?.status)
    }

    @Test
    fun anUnreadableAppliedAtKeepsTheApplication() = runTest {
        // Dropping the application would offer 신청하기 to someone who has already applied.
        val json = listJson.replace("2026-09-14T10:00:00Z", "yesterday")

        val result = TrainingRepositoryImpl(mockClient(json)).getTrainings()

        val first = (assertIs<TrainingResult.Success<*>>(result).data as List<*>).first()
            as com.hanmaum.dn.mobile.features.training.domain.model.Training
        val application = assertNotNull(first.myApplication)
        assertNull(application.appliedAt)
    }
}
