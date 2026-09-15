package com.hanmaum.dn.mobile.features.training.presentation.detail

import com.hanmaum.dn.mobile.features.login.domain.model.BirthDateInput
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicantPrefill
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicationField
import com.hanmaum.dn.mobile.features.training.domain.model.FormFieldOption
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingCourse

/*
 * The 양육 application form (#174, #242), kept free of Compose so every rule here is
 * unit-tested. Figma: section `22 · 양육 신청 · Zustände`, boards `Formular`,
 * `Formular-Zustände` and `Dynamische Felder`.
 */

/** How a field is edited. Decided by its options and its name; the external API documents only `enum`. */
enum class FieldKind { TEXT, EMAIL, PHONE, DATE, MULTILINE, CHOICE }

data class FormFieldUi(
    val field: ApplicationField,
    /** The external API's label; null means the app's own wording for [field]. */
    val serverLabel: String?,
    val required: Boolean,
    val kind: FieldKind,
    /** Option labels may be null for the known option sets; the screen names those itself. */
    val options: List<FormFieldOption>,
    /** A profile value this form may show but not change. */
    val locked: Boolean = false,
)

sealed interface FieldError {
    data object Required : FieldError
    data object InvalidEmail : FieldError
    data object InvalidDate : FieldError
    data object InvalidPhone : FieldError
    data object InvalidChoice : FieldError

    /** The server's message for this field, shown as it came. */
    data class Server(val message: String) : FieldError
}

/** What the last attempt to send ended in; drives the banner and the button. */
sealed interface FormOutcome {
    data class Success(val courseName: String) : FormOutcome
    data object Unavailable : FormOutcome
    data object Full : FormOutcome
    data object Closed : FormOutcome
    data object AlreadyApplied : FormOutcome
    data class NotEligible(val message: String?) : FormOutcome
    data class Invalid(val message: String?) : FormOutcome
    data object Failed : FormOutcome
}

data class ApplicationFormState(
    val course: TrainingCourse,
    /** In display order: see [ApplicationField]. */
    val fields: List<FormFieldUi>,
    val values: Map<ApplicationField, String>,
    /** Required fields the profile had nothing for; pointed out before the member sends. */
    val missingFromProfile: Set<ApplicationField>,
    val errors: Map<ApplicationField, FieldError> = emptyMap(),
    val consent: Boolean = false,
    val isSubmitting: Boolean = false,
    val outcome: FormOutcome? = null,
) {
    /** Outcomes after which sending the same form again cannot succeed. */
    val isBlocked: Boolean
        get() = outcome is FormOutcome.Full || outcome is FormOutcome.Closed || outcome is FormOutcome.NotEligible

    val canSubmit: Boolean
        get() = consent && !isSubmitting && !isBlocked &&
            outcome !is FormOutcome.AlreadyApplied && outcome !is FormOutcome.Success

    fun isLocked(field: ApplicationField): Boolean = fields.any { it.field == field && it.locked }

    /** Non-empty sections in display order. */
    fun groups(): List<Pair<ApplicationField.Group, List<FormFieldUi>>> =
        ApplicationField.Group.entries
            .map { group -> group to fields.filter { it.field.group == group } }
            .filter { (_, inGroup) -> inGroup.isNotEmpty() }
}

object ApplicationForms {

    /** Fields the profile can fill; only these can be "missing from the profile". */
    private val PREFILLABLE = setOf(
        ApplicationField.NAME,
        ApplicationField.BIRTH_DATE,
        ApplicationField.EMAIL,
        ApplicationField.PHONE,
        ApplicationField.GENDER,
        ApplicationField.RESIDENCE,
    )

    /**
     * Shown from the profile and never edited here. When the profile has no value, the field
     * stays editable: the server needs one, and a locked empty field would make applying
     * impossible.
     */
    private val LOCKABLE = setOf(ApplicationField.NAME, ApplicationField.BIRTH_DATE)

    private val MULTILINE = setOf(
        ApplicationField.CHILDREN,
        ApplicationField.HISTORY,
        ApplicationField.WAITING,
        ApplicationField.RUNNING,
        ApplicationField.COMMENT,
    )

    /**
     * The codes of the legacy application form (application.hanmaum.de `index.php`), which the
     * documented API uses unchanged. The live deployment sends no options at all, and a field
     * with a fixed set of values must never turn into free text (#242). Options the server does
     * send always win. Labels are the screen's: see `optionLabel` in ApplicationSheet.
     */
    val KNOWN_OPTIONS: Map<ApplicationField, List<FormFieldOption>> = mapOf(
        ApplicationField.GENDER to listOf("F", "M"),
        ApplicationField.BAPTIZED to listOf("1", "2", "3", "4"),
        ApplicationField.BAPTIZE_TYPE to listOf("1", "2", "3", "4", "5"),
    ).mapValues { (_, values) -> values.map { FormFieldOption(value = it, label = null) } }

    // Deliberately loose: the server and the external API validate the address for real.
    private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    private const val MIN_PHONE_DIGITS = 6

    /**
     * The form for [course], started from [prefill].
     *
     * A field whose name this app cannot send is left out: showing it would collect a value
     * that goes nowhere. If the external API requires it, the server answers INVALID.
     */
    fun open(course: TrainingCourse, prefill: ApplicantPrefill?): ApplicationFormState {
        val listed = course.formFields.mapNotNull { formField ->
            ApplicationField.fromWire(formField.name)?.let { field ->
                val options = formField.options.ifEmpty { KNOWN_OPTIONS[field].orEmpty() }
                FormFieldUi(
                    field = field,
                    serverLabel = formField.label?.takeIf { it.isNotBlank() },
                    required = formField.required,
                    kind = kindOf(field, options),
                    options = options,
                )
            }
        }.distinctBy { it.field }.sortedBy { it.field.ordinal }
        val asked = listed.map { it.field }.toSet()

        val values = buildMap {
            prefill?.name?.let { put(ApplicationField.NAME, it) }
            prefill?.birthDate?.let { put(ApplicationField.BIRTH_DATE, BirthDateInput.format(it)) }
            prefill?.email?.let { put(ApplicationField.EMAIL, it) }
            prefill?.phone?.let { put(ApplicationField.PHONE, it) }
            prefill?.gender?.let { put(ApplicationField.GENDER, it) }
            prefill?.residence?.let { put(ApplicationField.RESIDENCE, it) }
        }.filter { (field, value) -> field in asked && value.isNotBlank() }

        val fields = listed.map { ui -> ui.copy(locked = ui.field in LOCKABLE && ui.field in values) }

        val missing = fields
            .filter { it.required && it.field in PREFILLABLE && values[it.field].isNullOrBlank() }
            .map { it.field }
            .toSet()

        return ApplicationFormState(course = course, fields = fields, values = values, missingFromProfile = missing)
    }

    fun validate(form: ApplicationFormState): Map<ApplicationField, FieldError> =
        form.fields.filterNot { it.locked }.mapNotNull { ui ->
            val value = form.values[ui.field]?.trim().orEmpty()
            val error = when {
                value.isEmpty() -> FieldError.Required.takeIf { ui.required }
                ui.kind == FieldKind.EMAIL && !EMAIL_PATTERN.matches(value) -> FieldError.InvalidEmail
                ui.kind == FieldKind.DATE && isoDate(value) == null -> FieldError.InvalidDate
                ui.kind == FieldKind.PHONE &&
                    (value.any { it.isLetter() } || value.count { it.isDigit() } < MIN_PHONE_DIGITS) -> FieldError.InvalidPhone
                ui.kind == FieldKind.CHOICE && ui.options.none { it.value == value } -> FieldError.InvalidChoice
                else -> null
            }
            error?.let { ui.field to it }
        }.toMap()

    /**
     * What is sent: trimmed, blanks left to the server's profile fallback, the birth date as
     * YYYY-MM-DD. Locked fields are left out too — they are the profile's values, which the
     * server takes itself.
     */
    fun requestValues(form: ApplicationFormState): Map<ApplicationField, String> =
        form.fields.filterNot { it.locked }.mapNotNull { ui ->
            val value = form.values[ui.field]?.trim().orEmpty()
            when {
                value.isEmpty() -> null
                ui.kind == FieldKind.DATE -> ui.field to (isoDate(value) ?: value)
                else -> ui.field to value
            }
        }.toMap()

    private fun kindOf(field: ApplicationField, options: List<FormFieldOption>): FieldKind = when {
        options.isNotEmpty() -> FieldKind.CHOICE
        field == ApplicationField.BIRTH_DATE -> FieldKind.DATE
        field == ApplicationField.EMAIL -> FieldKind.EMAIL
        field == ApplicationField.PHONE -> FieldKind.PHONE
        field in MULTILINE -> FieldKind.MULTILINE
        else -> FieldKind.TEXT
    }

    // The field shows YYYY.MM.DD and accepts seven or eight digits; see BirthDateInput.
    private fun isoDate(text: String): String? =
        BirthDateInput.parse(BirthDateInput.normalise(text))?.toString()
}
