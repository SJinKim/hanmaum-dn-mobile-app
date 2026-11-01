package com.hanmaum.dn.mobile.features.member.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiErrorResponse
import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.data.model.UpdateMyProfileRequest
import com.hanmaum.dn.mobile.features.member.domain.model.PreparedProfileImage
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileLoadException
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageException
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.delete
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

private const val PROFILE_IMAGE_PATH = "members/me/profile-image"

class MemberRepositoryImpl(
    private val client: HttpClient
) : MemberRepository {

    override suspend fun getMyProfile(): Result<MemberResponse> {
        return try {
            val response = client.get("members/me") {
                contentType(ContentType.Application.Json)
            }
            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<ApiResponse<MemberResponse>>()
                val member = apiResponse.data
                    ?: return Result.failure(ProfileLoadException("Profile data is null"))
                Result.success(member)
            } else {
                Result.failure(ProfileLoadException("Profile load failed (${response.status})"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateMyProfile(
        phoneNumber: String?,
        birthDate: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
        identityStatus: String?,
        workOrSchool: String?,
    ): Result<MemberResponse> {
        return try {
            val response = client.patch("members/me") {
                contentType(ContentType.Application.Json)
                setBody(UpdateMyProfileRequest(
                    phoneNumber = phoneNumber,
                    birthDate = birthDate,
                    street = street,
                    houseNumber = houseNumber,
                    zipCode = zipCode,
                    city = city,
                    identityStatus = identityStatus,
                    workOrSchool = workOrSchool,
                ))
            }
            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<ApiResponse<MemberResponse>>()
                val member = apiResponse.data
                    ?: return Result.failure(Exception("Profile data is null"))
                Result.success(member)
            } else {
                Result.failure(Exception("Profile update failed (${response.status})"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun uploadProfileImage(
        image: PreparedProfileImage,
        onProgress: (sent: Long, total: Long?) -> Unit,
    ): Result<MemberResponse> = profileImageCall {
        client.put(PROFILE_IMAGE_PATH) {
            // Ktor writes the multipart Content-Type with its boundary itself.
            // The provider re-opens the file on every write, so a 401 replay
            // after a token refresh sends the whole image again.
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file",
                            InputProvider(image.byteLength) { image.open() },
                            Headers.build {
                                append(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                                append(HttpHeaders.ContentDisposition, "filename=\"profile.jpg\"")
                            },
                        )
                    },
                ),
            )
            onUpload { sent, total -> onProgress(sent, total) }
        }
    }

    override suspend fun deleteProfileImage(): Result<MemberResponse> =
        profileImageCall { client.delete(PROFILE_IMAGE_PATH) }

    /**
     * Shared envelope for the two avatar mutations: success needs 200,
     * `success == true` and a body. Nothing is logged — a stack trace or a
     * server message could carry a signed URL.
     */
    private suspend fun profileImageCall(send: suspend () -> HttpResponse): Result<MemberResponse> {
        val response = try {
            send()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return failure(ProfileImageFailure.NETWORK)
        }
        if (response.status != HttpStatusCode.OK) {
            val code = try {
                response.body<ApiErrorResponse>().code
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            return failure(profileImageFailure(response.status.value, code))
        }
        val member = try {
            response.body<ApiResponse<MemberResponse>>().takeIf { it.success }?.data
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return member?.let { Result.success(it) } ?: failure(ProfileImageFailure.UNKNOWN)
    }

    private fun failure(kind: ProfileImageFailure): Result<MemberResponse> =
        Result.failure(ProfileImageException(kind))
}

/** Stable server codes first; the status is only the fallback for a missing or unknown code. */
internal fun profileImageFailure(status: Int, code: String?): ProfileImageFailure = when (code) {
    "PROFILE_IMAGE_TOO_LARGE" -> ProfileImageFailure.TOO_LARGE
    "PROFILE_IMAGE_UNSUPPORTED_TYPE" -> ProfileImageFailure.UNSUPPORTED_TYPE
    "PROFILE_IMAGE_INVALID" -> ProfileImageFailure.INVALID_IMAGE
    "PROFILE_IMAGE_STORAGE_UNAVAILABLE" -> ProfileImageFailure.STORAGE_UNAVAILABLE
    else -> when (status) {
        401 -> ProfileImageFailure.AUTHENTICATION_REQUIRED
        403 -> ProfileImageFailure.ACCESS_DENIED
        404 -> ProfileImageFailure.PROFILE_UNAVAILABLE
        413 -> ProfileImageFailure.TOO_LARGE
        415 -> ProfileImageFailure.UNSUPPORTED_TYPE
        422 -> ProfileImageFailure.INVALID_IMAGE
        503 -> ProfileImageFailure.STORAGE_UNAVAILABLE
        else -> ProfileImageFailure.UNKNOWN
    }
}
