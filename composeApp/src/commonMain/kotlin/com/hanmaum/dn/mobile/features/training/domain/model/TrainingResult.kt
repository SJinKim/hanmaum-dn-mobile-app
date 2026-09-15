package com.hanmaum.dn.mobile.features.training.domain.model

/** Typed outcome of a 양육 call, keeping Ktor and HTTP status codes out of the ViewModels. */
sealed interface TrainingResult<out T> {
    data class Success<T>(val data: T) : TrainingResult<T>

    /**
     * application.hanmaum.de cannot be asked (503 COURSE_APPLICATION_UNAVAILABLE).
     *
     * Nothing the member did and nothing a retry fixes soon, so it is shown as 준비중입니다.,
     * never as an error.
     */
    data object Unavailable : TrainingResult<Nothing>

    data object Failed : TrainingResult<Nothing>
}
