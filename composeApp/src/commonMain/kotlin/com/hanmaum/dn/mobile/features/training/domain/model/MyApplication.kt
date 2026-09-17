package com.hanmaum.dn.mobile.features.training.domain.model

import kotlin.time.Instant

/**
 * One 양육 application of the signed-in member, as 나의 신청 lists it.
 *
 * [TrainingApplication] hangs off a training the member already has open, so it can stay
 * silent about which training it belongs to. This one stands on its own — the list is built
 * from GET /me/trainings alone (hanmaum-dn-server#169) — and therefore carries the training's
 * name and id, the latter so tapping an entry can open the 양육 detail page.
 */
data class MyApplication(
    val trainingPublicId: String,
    /** The Korean name where the server has one, so the list reads like the rest of 양육. */
    val trainingName: String,
    /**
     * Part of the list key: cancelling and applying again makes a second application for the
     * same training, so the training id alone does not identify a row.
     */
    val externalCourseId: Int,
    /** 신청한 반, e.g. 큐베세 직장인/청년 반. Null when the server sends none. */
    val courseName: String?,
    /** Null only when the server sent a timestamp this client cannot read. */
    val appliedAt: Instant?,
    val status: TrainingApplicationStatus,
)
