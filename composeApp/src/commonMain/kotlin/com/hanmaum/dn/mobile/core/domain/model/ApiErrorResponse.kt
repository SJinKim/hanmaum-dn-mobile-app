package com.hanmaum.dn.mobile.core.domain.model

import kotlinx.serialization.Serializable

/**
 * The server's error body (`ErrorResponse`). It is not wrapped in [ApiResponse].
 *
 * [code] is the only field a client may branch on: `message` is prose the server is free
 * to reword. [fieldErrors] maps request property names to messages where an error concerns
 * specific fields, and is null otherwise.
 */
@Serializable
data class ApiErrorResponse(
    val status: Int? = null,
    val message: String? = null,
    val code: String? = null,
    val fieldErrors: Map<String, String>? = null,
)
