package com.hanmaum.dn.mobile.features.training.domain.model

/**
 * Typed outcome of applying for a 양육 course, one case per error code the server sends
 * (hanmaum-dn-server#167). The code decides, never the HTTP status: a 409 alone can mean
 * closed, full or already applied.
 */
sealed interface ApplyResult {
    /** Applied, or the application already made for a retried request. */
    data class Success(val courseName: String) : ApplyResult

    /** 503 COURSE_APPLICATION_UNAVAILABLE: shown as 준비중입니다., the input stays. */
    data object Unavailable : ApplyResult

    /** 409 COURSE_APPLICATION_FULL. */
    data object Full : ApplyResult

    /** 409 COURSE_APPLICATION_CLOSED: the window closed between loading and sending. */
    data object Closed : ApplyResult

    /** 409 COURSE_APPLICATION_ALREADY_APPLIED, e.g. applied from a second device. */
    data object AlreadyApplied : ApplyResult

    /**
     * 403 COURSE_APPLICATION_NOT_ELIGIBLE or 404 COURSE_APPLICATION_COURSE_NOT_FOUND.
     * [message] is the server's text, written for members.
     */
    data class NotEligible(val message: String?) : ApplyResult

    /**
     * COURSE_APPLICATION_INVALID, or a 400 from request validation, which carries no code.
     * [fieldErrors] holds only fields this app knows. [message] is the server's text when it
     * named no field, e.g. a course that cannot be applied to through the app.
     */
    data class Invalid(val fieldErrors: Map<ApplicationField, String>, val message: String?) : ApplyResult

    data object Failed : ApplyResult
}
