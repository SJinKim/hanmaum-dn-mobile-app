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
    /** What the UI hands Coil to show the unsaved preview. Never persisted or logged. */
    val preview: Any
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
    /** The picker handed back something unreadable; only a new pick helps. */
    UNREADABLE,
    UNKNOWN,
}

class ProfileImageException(val failure: ProfileImageFailure) : Exception("Profile image failed: $failure")

/**
 * GET members/me reached the server and got an unusable answer. Any other
 * failure of that call never reached it — which is how the profile tells
 * "offline" apart from "the server said no".
 */
class ProfileLoadException(message: String) : Exception(message)
