package com.hanmaum.dn.mobile.features.member.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import com.hanmaum.dn.mobile.features.member.domain.model.PreparedProfileImage
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageException
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.io.Buffer
import kotlinx.io.Source

private val testJson = Json { ignoreUnknownKeys = true }

private class FakePreparedImage(private val bytes: ByteArray) : PreparedProfileImage {
    var opened = 0
    var released = false
    override val byteLength: Long = bytes.size.toLong()
    override fun open(): Source { opened++; return Buffer().apply { write(bytes) } }
    override fun release() { released = true }
}

private fun statusClient(
    status: HttpStatusCode,
    body: String,
    onRequest: (suspend (HttpRequestData) -> Unit)? = null,
): HttpClient = HttpClient(MockEngine { request ->
    onRequest?.invoke(request)
    respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    install(ContentNegotiation) { json(testJson) }
    defaultRequest {
        if (url.host.isBlank()) {
            val path = url.encodedPath.removePrefix("/")
            url.takeFrom("http://localhost")
            url.encodedPath = "/$path"
        }
    }
}

private fun mockClient(
    responseJson: String,
    onRequest: ((HttpRequestData) -> Unit)? = null,
): HttpClient = HttpClient(MockEngine { request ->
    onRequest?.invoke(request)
    respond(
        content = responseJson,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    install(ContentNegotiation) { json(testJson) }
    defaultRequest {
        if (url.host.isBlank()) {
            val path = url.encodedPath.removePrefix("/")
            url.takeFrom("http://localhost")
            url.encodedPath = "/$path"
        }
    }
}

class MemberRepositoryImplTest {

    private val member = MemberResponse(
        publicId = "u1",
        firstName = "Seungjin",
        lastName = "Kim",
        status = MemberStatus.ACTIVE,
        groupName = "다니엘조",
        division = "2교구",
        street = "Musterstraße",
        houseNumber = "12",
        zipCode = "50667",
        city = "Köln",
        birthDate = "1992-12-07",
    )

    @Test
    fun getMyProfile_mapsHouseNumber() = runTest {
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val result = MemberRepositoryImpl(mockClient(json)).getMyProfile()
        val p = result.getOrThrow()
        assertEquals("Musterstraße", p.street)
        assertEquals("12", p.houseNumber)
    }

    @Test
    fun updateMyProfile_patchesMembersMe() = runTest {
        var method: HttpMethod? = null
        var path = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req ->
            method = req.method
            path = req.url.encodedPath
        }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, birthDate = null,
            street = "Musterstraße", houseNumber = "12", zipCode = "50667", city = "Köln",
            identityStatus = null, workOrSchool = null,
        )
        assertEquals(HttpMethod.Patch, method)
        assertEquals("/members/me", path)
    }

    @Test
    fun updateMyProfile_sendsHouseNumberAndCamelCaseZipCode() = runTest {
        // Regression for the Hausnummer bug: the old request DTO had no
        // houseNumber field at all and misnamed zipCode as zip_code (the
        // backend is default-Jackson camelCase, so zip_code was silently
        // dropped server-side).
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, birthDate = null,
            street = "Musterstraße", houseNumber = "12", zipCode = "50667", city = "Köln",
            identityStatus = null, workOrSchool = null,
        )
        assertTrue(body.contains("\"houseNumber\":\"12\""), "body was $body")
        assertTrue(body.contains("\"zipCode\":\"50667\""), "body was $body")
        assertFalse(body.contains("zip_code"), "body was $body")
    }

    @Test
    fun updateMyProfile_omitsNullFieldsFromBody() = runTest {
        // Backend PATCH semantics are null-means-keep; kotlinx must OMIT null
        // fields (encodeDefaults=false), not send explicit nulls.
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, birthDate = null,
            street = null, houseNumber = null, zipCode = null, city = "Köln",
            identityStatus = null, workOrSchool = null,
        )
        assertFalse(body.contains("houseNumber"), "body was $body")
        assertFalse(body.contains("street"), "body was $body")
        assertTrue(body.contains("\"city\":\"Köln\""), "body was $body")
    }

    @Test
    fun getMyProfile_mapsDivisionAndBirthDate() = runTest {
        val json = """
            {"success":true,"data":{"publicId":"u1","firstName":"Seungjin","lastName":"Kim",
             "status":"ACTIVE","groupName":"다니엘조","division":"2교구","birthDate":"1992-12-07"}}
        """.trimIndent()
        val p = MemberRepositoryImpl(mockClient(json)).getMyProfile().getOrThrow()
        assertEquals("2교구", p.division)
        assertEquals("1992-12-07", p.birthDate)
    }

    @Test
    fun getMyProfile_mapsActiveMinistries() = runTest {
        // Feeds the profile's "소속 사역" count (#160).
        val json = """
            {"success":true,"data":{"publicId":"u1","firstName":"Seungjin","lastName":"Kim",
             "status":"ACTIVE","activeMinistries":["찬양팀","미디어팀"]}}
        """.trimIndent()
        val p = MemberRepositoryImpl(mockClient(json)).getMyProfile().getOrThrow()
        assertEquals(listOf("찬양팀", "미디어팀"), p.activeMinistries)
    }

    @Test
    fun getMyProfile_mapsRegistrationDate() = runTest {
        // Feeds the profile's "함께한 시간" tile (#114). The server sends it as
        // an ISO date under exactly this name — MemberResponse.registrationDate.
        val json = """
            {"success":true,"data":{"publicId":"u1","firstName":"Seungjin","lastName":"Kim",
             "status":"ACTIVE","registrationDate":"2022-03-02"}}
        """.trimIndent()
        val p = MemberRepositoryImpl(mockClient(json)).getMyProfile().getOrThrow()
        assertEquals("2022-03-02", p.registrationDate)
    }

    @Test
    fun getMyProfile_toleratesOldBackendWithoutNewFields() = runTest {
        val json = """
            {"success":true,"data":{"publicId":"u1","firstName":"Seungjin","lastName":"Kim","status":"ACTIVE"}}
        """.trimIndent()
        val p = MemberRepositoryImpl(mockClient(json)).getMyProfile().getOrThrow()
        assertEquals(null, p.division)
        assertEquals(null, p.birthDate)
        assertEquals(null, p.registrationDate)
        assertEquals(emptyList(), p.activeMinistries)
    }

    @Test
    fun updateMyProfile_sendsBirthDateKeyCamelCase() = runTest {
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, birthDate = "1992-12-07",
            street = null, houseNumber = null, zipCode = null, city = null,
            identityStatus = null, workOrSchool = null,
        )
        assertTrue(body.contains("\"birthDate\":\"1992-12-07\""), "body was $body")
    }

    @Test
    fun updateMyProfile_omitsNullBirthDate() = runTest {
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, birthDate = null,
            street = null, houseNumber = null, zipCode = null, city = "Köln",
            identityStatus = null, workOrSchool = null,
        )
        assertFalse(body.contains("birthDate"), "body was $body")
    }

    @Test
    fun updateMyProfile_sendsIdentityStatusAndWorkOrSchool() = runTest {
        // #283: 신분 and 소속 are editable from 프로필 수정 under these wire names.
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, birthDate = null,
            street = null, houseNumber = null, zipCode = null, city = null,
            identityStatus = "UNIVERSITY_STUDENT", workOrSchool = "TU Berlin",
        )
        assertTrue(body.contains("\"identityStatus\":\"UNIVERSITY_STUDENT\""), "body was $body")
        assertTrue(body.contains("\"workOrSchool\":\"TU Berlin\""), "body was $body")
    }

    @Test
    fun getMyProfile_mapsIdentityStatusAndWorkOrSchool() = runTest {
        val json = """
            {"success":true,"data":{"publicId":"u1","firstName":"Seungjin","lastName":"Kim",
             "status":"ACTIVE","identityStatus":"EMPLOYEE","workOrSchool":"Siemens"}}
        """.trimIndent()
        val p = MemberRepositoryImpl(mockClient(json)).getMyProfile().getOrThrow()
        assertEquals("EMPLOYEE", p.identityStatus)
        assertEquals("Siemens", p.workOrSchool)
    }
    @Test
    fun getMyProfile_mapsProfileImageVersionAndDefaultsToZero() = runTest {
        val withVersion = """
            {"success":true,"data":{"publicId":"u1","firstName":"S","lastName":"K","status":"ACTIVE",
             "profileImageUrl":"https://storage.example/a.jpg?sig=1","profileImageVersion":4}}
        """.trimIndent()
        val p = MemberRepositoryImpl(mockClient(withVersion)).getMyProfile().getOrThrow()
        assertEquals(4L, p.profileImageVersion)
        assertEquals("https://storage.example/a.jpg?sig=1", p.profileImageUrl)

        val without = """{"success":true,"data":{"publicId":"u1","firstName":"S","lastName":"K","status":"ACTIVE"}}"""
        assertEquals(0L, MemberRepositoryImpl(mockClient(without)).getMyProfile().getOrThrow().profileImageVersion)
    }

    @Test
    fun updateMyProfile_neverSendsAnImageField() = runTest {
        // The image moved to its own endpoint; the server ignores it in PATCH (#139).
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = "+491511234567", birthDate = null,
            street = null, houseNumber = null, zipCode = null, city = "Köln",
            identityStatus = null, workOrSchool = null,
        )
        assertFalse(body.contains("profileImage"), "body was $body")
    }

    @Test
    fun uploadProfileImage_putsOneJpegFilePartAndReturnsTheServerProfile() = runTest {
        val image = FakePreparedImage(byteArrayOf(1, 2, 3, 4, 5))
        var method: HttpMethod? = null
        var path = ""
        var contentType: ContentType? = null
        var body = ""
        val updated = member.copy(profileImageUrl = "https://storage.example/p.jpg?sig=2", profileImageVersion = 3)
        val client = statusClient(HttpStatusCode.OK, testJson.encodeToString(ApiResponse(success = true, data = updated))) { req ->
            method = req.method
            path = req.url.encodedPath
            contentType = req.body.contentType
            body = req.body.toByteArray().decodeToString()
        }
        val progress = mutableListOf<Long>()
        val result = MemberRepositoryImpl(client).uploadProfileImage(image) { sent, _ -> progress += sent }

        assertEquals(HttpMethod.Put, method)
        assertEquals("/members/me/profile-image", path)
        assertTrue(contentType!!.match(ContentType.MultiPart.FormData), "was $contentType")
        assertTrue(contentType!!.parameter("boundary") != null, "multipart needs Ktor's boundary")
        assertTrue(body.contains("name=file"), "body was $body")
        assertTrue(body.contains("filename=\"profile.jpg\""), "body was $body")
        assertTrue(body.contains("Content-Type: image/jpeg"), "body was $body")
        assertTrue(body.contains("Content-Length: 5"), "body was $body")
        assertEquals(1, image.opened)
        assertEquals(3L, result.getOrThrow().profileImageVersion)
        assertTrue(progress.isNotEmpty())
    }

    @Test
    fun uploadProfileImage_mapsStableErrorCodes() = runTest {
        val cases = listOf(
            Triple(HttpStatusCode.PayloadTooLarge, "PROFILE_IMAGE_TOO_LARGE", ProfileImageFailure.TOO_LARGE),
            Triple(HttpStatusCode.UnsupportedMediaType, "PROFILE_IMAGE_UNSUPPORTED_TYPE", ProfileImageFailure.UNSUPPORTED_TYPE),
            Triple(HttpStatusCode.UnprocessableEntity, "PROFILE_IMAGE_INVALID", ProfileImageFailure.INVALID_IMAGE),
            Triple(HttpStatusCode.ServiceUnavailable, "PROFILE_IMAGE_STORAGE_UNAVAILABLE", ProfileImageFailure.STORAGE_UNAVAILABLE),
            Triple(HttpStatusCode.Forbidden, "CONSENT_REQUIRED", ProfileImageFailure.ACCESS_DENIED),
        )
        for ((status, code, expected) in cases) {
            val client = statusClient(status, """{"status":${status.value},"message":"x","code":"$code"}""")
            val error = MemberRepositoryImpl(client).uploadProfileImage(FakePreparedImage(byteArrayOf(1))) { _, _ -> }
                .exceptionOrNull()
            assertEquals(expected, assertIs<ProfileImageException>(error).failure, "for $code")
        }
    }

    @Test
    fun uploadProfileImage_fallsBackToStatusWithoutACode() = runTest {
        val cases = mapOf(
            HttpStatusCode.Unauthorized to ProfileImageFailure.AUTHENTICATION_REQUIRED,
            HttpStatusCode.NotFound to ProfileImageFailure.PROFILE_UNAVAILABLE,
            HttpStatusCode.PayloadTooLarge to ProfileImageFailure.TOO_LARGE,
            HttpStatusCode.InternalServerError to ProfileImageFailure.UNKNOWN,
        )
        for ((status, expected) in cases) {
            val client = statusClient(status, "not json")
            val error = MemberRepositoryImpl(client).uploadProfileImage(FakePreparedImage(byteArrayOf(1))) { _, _ -> }
                .exceptionOrNull()
            assertEquals(expected, assertIs<ProfileImageException>(error).failure, "for $status")
        }
    }

    @Test
    fun uploadProfileImage_treatsAnUnsuccessfulEnvelopeAsFailure() = runTest {
        val client = statusClient(HttpStatusCode.OK, """{"success":false,"data":null}""")
        val error = MemberRepositoryImpl(client).uploadProfileImage(FakePreparedImage(byteArrayOf(1))) { _, _ -> }
            .exceptionOrNull()
        assertEquals(ProfileImageFailure.UNKNOWN, assertIs<ProfileImageException>(error).failure)
    }

    @Test
    fun uploadProfileImage_mapsTransportErrorsToNetwork() = runTest {
        val client = HttpClient(MockEngine { throw IOException("offline") }) {
            install(ContentNegotiation) { json(testJson) }
        }
        val error = MemberRepositoryImpl(client).uploadProfileImage(FakePreparedImage(byteArrayOf(1))) { _, _ -> }
            .exceptionOrNull()
        assertEquals(ProfileImageFailure.NETWORK, assertIs<ProfileImageException>(error).failure)
    }

    @Test
    fun uploadProfileImage_rethrowsCancellation() = runTest {
        val client = HttpClient(MockEngine { throw CancellationException("left the screen") }) {
            install(ContentNegotiation) { json(testJson) }
        }
        assertFailsWith<CancellationException> {
            MemberRepositoryImpl(client).uploadProfileImage(FakePreparedImage(byteArrayOf(1))) { _, _ -> }
        }
    }

    @Test
    fun deleteProfileImage_deletesAndReturnsTheProfileWithoutImage() = runTest {
        var method: HttpMethod? = null
        var path = ""
        val cleared = member.copy(profileImageUrl = null, profileImageVersion = 5)
        val client = statusClient(HttpStatusCode.OK, testJson.encodeToString(ApiResponse(success = true, data = cleared))) { req ->
            method = req.method
            path = req.url.encodedPath
        }
        val p = MemberRepositoryImpl(client).deleteProfileImage().getOrThrow()
        assertEquals(HttpMethod.Delete, method)
        assertEquals("/members/me/profile-image", path)
        assertEquals(null, p.profileImageUrl)
        assertEquals(5L, p.profileImageVersion)
    }

    @Test
    fun deleteProfileImage_mapsStorageUnavailable() = runTest {
        val client = statusClient(
            HttpStatusCode.ServiceUnavailable,
            """{"status":503,"code":"PROFILE_IMAGE_STORAGE_UNAVAILABLE"}""",
        )
        val error = MemberRepositoryImpl(client).deleteProfileImage().exceptionOrNull()
        assertEquals(ProfileImageFailure.STORAGE_UNAVAILABLE, assertIs<ProfileImageException>(error).failure)
    }
}
