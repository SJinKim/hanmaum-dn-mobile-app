package com.hanmaum.dn.mobile.features.login.domain.model

/**
 * 신분 — what a member does in Germany, required at registration (#282).
 *
 * The names are the server's `IdentityStatus` enum and go over the wire as
 * they are; the order is the order the picker lists them in, the same as
 * the web app's 새가족 page.
 */
enum class IdentityStatus {
    EMPLOYEE,
    UNIVERSITY_STUDENT,
    EXAM_PREPARATION,
    WORKING_HOLIDAY,
    EXCHANGE_STUDENT,
    SELF_EMPLOYED,
    EXPATRIATE,
    JOB_SEEKING,
}
