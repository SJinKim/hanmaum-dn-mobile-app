package com.hanmaum.dn.mobile.features.member.domain.model

import kotlinx.io.Source

/**
 * The avatar the server has confirmed. Only a successful GET/PUT/DELETE
 * response produces one; a picked or uploading image never does.
 *
 * [url] is a presigned GET that expires, so it is never the identity of the
 * image: [cacheKey] is. It changes only when the server bumps [version].
 */
data class ProfileAvatar(
    val memberPublicId: String,
    val version: Long,
    val url: String?,
) {
    val cacheKey: String get() = cacheKey(memberPublicId, version)

    companion object {
        fun cacheKey(memberPublicId: String, version: Long): String = "profile:$memberPublicId:$version"
    }
}

/**
 * A JPEG ready to upload, backed by a private temporary file.
 *
 * [open] hands out a fresh stream on every call, so an auth replay or an
 * explicit retry sends the whole file again instead of an exhausted stream.
 * [release] deletes the file and is safe to call more than once.
 */
interface PreparedProfileImage {
    val byteLength: Long
    fun open(): Source
    fun release()
}

/** Why an avatar operation failed. Mapped from stable server codes, never from prose. */
enum class ProfileImageFailure {
    TOO_LARGE,
    UNSUPPORTED_TYPE,
    INVALID_IMAGE,
    STORAGE_UNAVAILABLE,
    AUTHENTICATION_REQUIRED,
    ACCESS_DENIED,
    PROFILE_UNAVAILABLE,
    NETWORK,
    UNKNOWN,
}

class ProfileImageException(val failure: ProfileImageFailure) : Exception("Profile image failed: $failure")
