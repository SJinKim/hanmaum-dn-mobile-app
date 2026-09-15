package com.hanmaum.dn.mobile.features.training.presentation.detail

import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicantPrefill
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicationField
import com.hanmaum.dn.mobile.features.training.domain.model.CourseFormField
import com.hanmaum.dn.mobile.features.training.domain.model.FormFieldOption
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApplicationFormsTest {

    private fun field(
        name: String,
        required: Boolean = false,
        label: String? = null,
        options: List<FormFieldOption> = emptyList(),
    ) = CourseFormField(name = name, type = if (options.isEmpty()) null else "enum", required = required, label = label, options = options)

    private val gender = field(
        "gender",
        required = true,
        label = "성별",
        options = listOf(FormFieldOption("F", "여성"), FormFieldOption("M", "남성")),
    )

    private fun course(vararg fields: CourseFormField) = FakeTrainingRepository.course(106, formFields = fields.toList())

    private val prefill = ApplicantPrefill(
        name = "김한마음",
        birthDate = LocalDate(1995, 3, 14),
        email = "hanmaum@example.com",
        phone = null,
        gender = "M",
        residence = "뒤셀도르프",
    )

    @Test
    fun theFormShowsTheServersFieldsInSectionsAndSkipsUnknownOnes() {
        val form = ApplicationForms.open(
            course(field("history"), field("phone", true), field("name", true), gender, field("aSomethingNew", true), field("birthDate", true)),
            prefill,
        )

        assertEquals(
            listOf(ApplicationField.Group.BASIC, ApplicationField.Group.CHURCH, ApplicationField.Group.EXPERIENCE),
            form.groups().map { it.first },
        )
        assertEquals(
            listOf(ApplicationField.PHONE, ApplicationField.NAME, ApplicationField.BIRTH_DATE),
            form.groups().first().second.map { it.field },
            "within a section the server's order is kept",
        )
        assertEquals(5, form.fields.size, "a field the app cannot send is not shown")

        val kinds = form.fields.associate { it.field to it.kind }
        assertEquals(FieldKind.PHONE, kinds[ApplicationField.PHONE])
        assertEquals(FieldKind.DATE, kinds[ApplicationField.BIRTH_DATE])
        assertEquals(FieldKind.CHOICE, kinds[ApplicationField.GENDER])
        assertEquals(FieldKind.MULTILINE, kinds[ApplicationField.HISTORY])
    }

    @Test
    fun theProfileFillsOnlyTheFieldsTheCourseAsksFor() {
        val form = ApplicationForms.open(course(field("name", true), field("birthDate", true), gender), prefill)

        assertEquals(
            mapOf(
                ApplicationField.NAME to "김한마음",
                ApplicationField.BIRTH_DATE to "1995.03.14",
                ApplicationField.GENDER to "M",
            ),
            form.values,
        )
    }

    @Test
    fun aRequiredValueTheProfileLacksIsPointedOut() {
        val form = ApplicationForms.open(course(field("phone", true), field("email", true), field("comment", true)), prefill)

        assertEquals(setOf(ApplicationField.PHONE), form.missingFromProfile)
    }

    @Test
    fun aBlankServerLabelFallsBackToTheAppsWording() {
        val form = ApplicationForms.open(course(field("phone", true, label = " ")), null)

        assertNull(form.fields.single().serverLabel)
    }

    @Test
    fun validationNamesWhatIsWrongWithEachField() {
        val form = ApplicationForms.open(
            course(field("name", true), field("birthDate", true), field("email", true), field("phone", true), gender, field("comment")),
            null,
        ).copy(
            values = mapOf(
                ApplicationField.BIRTH_DATE to "1995.02.30",
                ApplicationField.EMAIL to "hanmaum@example",
                ApplicationField.PHONE to "017O-12",
                ApplicationField.GENDER to "X",
            ),
        )

        assertEquals(
            mapOf(
                ApplicationField.NAME to FieldError.Required,
                ApplicationField.BIRTH_DATE to FieldError.InvalidDate,
                ApplicationField.EMAIL to FieldError.InvalidEmail,
                ApplicationField.PHONE to FieldError.InvalidPhone,
                ApplicationField.GENDER to FieldError.InvalidChoice,
            ),
            ApplicationForms.validate(form),
        )
    }

    @Test
    fun aCompleteFormPassesAndAnEmptyOptionalFieldIsFine() {
        val form = ApplicationForms.open(
            course(field("name", true), field("birthDate", true), field("email", true), field("phone", true), field("comment")),
            FakeTrainingRepository.PREFILL,
        )

        assertTrue(ApplicationForms.validate(form).isEmpty())
    }

    @Test
    fun whatIsSentIsTrimmedWithoutBlanksAndTheBirthDateInIsoForm() {
        val form = ApplicationForms.open(course(field("name", true), field("birthDate", true), field("comment"), field("history")), null)
            .copy(
                values = mapOf(
                    ApplicationField.NAME to "  김한마음 ",
                    ApplicationField.BIRTH_DATE to "19950314",
                    ApplicationField.COMMENT to "   ",
                ),
            )

        assertEquals(
            mapOf(ApplicationField.NAME to "김한마음", ApplicationField.BIRTH_DATE to "1995-03-14"),
            ApplicationForms.requestValues(form),
        )
    }
}
