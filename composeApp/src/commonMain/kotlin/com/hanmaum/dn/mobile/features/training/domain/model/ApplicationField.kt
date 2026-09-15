package com.hanmaum.dn.mobile.features.training.domain.model

/**
 * The applicant fields `POST /trainings/{publicId}/registrations` accepts, by their request
 * property name (hanmaum-dn-server's TrainingApplicationRequest).
 *
 * The server translates these to the external API's `a…` columns, so the app never sees those.
 * A form field whose name is not listed here cannot be sent and is not shown.
 *
 * **Declaration order is display order.** The form always shows 기본 정보, then 교회 정보,
 * then 양육 경험, each in the order below, whatever order the server lists the fields in
 * (Figma: section `22 · 양육 신청 · Zustände`, board `양육 신청 · Formular`; #242).
 */
enum class ApplicationField(val wireName: String, val group: Group) {
    NAME("name", Group.BASIC),
    BIRTH_DATE("birthDate", Group.BASIC),
    GENDER("gender", Group.BASIC),
    EMAIL("email", Group.BASIC),
    PHONE("phone", Group.BASIC),
    RESIDENCE("residence", Group.BASIC),
    BAPTIZED("baptized", Group.CHURCH),
    BAPTIZE_TYPE("baptizeType", Group.CHURCH),
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
