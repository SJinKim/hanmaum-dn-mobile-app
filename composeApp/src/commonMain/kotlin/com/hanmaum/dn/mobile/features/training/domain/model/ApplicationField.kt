package com.hanmaum.dn.mobile.features.training.domain.model

/**
 * The applicant fields `POST /trainings/{publicId}/registrations` accepts, by their request
 * property name (hanmaum-dn-server's TrainingApplicationRequest).
 *
 * The server translates these to the external API's `a…` columns, so the app never sees those.
 * A form field whose name is not listed here cannot be sent and is not shown.
 */
enum class ApplicationField(val wireName: String, val group: Group) {
    NAME("name", Group.BASIC),
    BIRTH_DATE("birthDate", Group.BASIC),
    EMAIL("email", Group.BASIC),
    PHONE("phone", Group.BASIC),
    GENDER("gender", Group.CHURCH),
    BAPTIZED("baptized", Group.CHURCH),
    BAPTIZE_TYPE("baptizeType", Group.CHURCH),
    RESIDENCE("residence", Group.CHURCH),
    GYOGU("gyogu", Group.CHURCH),
    SOON("soon", Group.CHURCH),
    CHILDREN("children", Group.CHURCH),
    HISTORY("history", Group.EXPERIENCE),
    WAITING("waiting", Group.EXPERIENCE),
    RUNNING("running", Group.EXPERIENCE),
    COMMENT("comment", Group.EXPERIENCE),
    ;

    /** The form's sections, in the order they appear. */
    enum class Group { BASIC, CHURCH, EXPERIENCE }

    companion object {
        fun fromWire(name: String): ApplicationField? = entries.firstOrNull { it.wireName == name }
    }
}
