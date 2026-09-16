package com.hanmaum.dn.mobile.features.training.presentation.detail

import com.hanmaum.dn.mobile.features.training.FakeTrainingRepository
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicantPrefill
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicationField
import com.hanmaum.dn.mobile.features.training.domain.model.CourseFormField
import com.hanmaum.dn.mobile.features.training.domain.model.FormFieldOption
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun theFormAlwaysShowsBasicThenChurchThenExperienceInTheAppsOrder() {
        // Server order deliberately scrambled: experience first, church before basic.
        val form = ApplicationForms.open(
            course(
                field("history"), field("baptized"), field("residence"), field("phone", true), gender,
                field("aSomethingNew", true), field("email", true), field("birthDate", true), field("name", true),
            ),
            prefill,
        )

        assertEquals(
            listOf(ApplicationField.Group.BASIC, ApplicationField.Group.CHURCH, ApplicationField.Group.EXPERIENCE),
            form.groups().map { it.first },
        )
        assertEquals(
            listOf(
                ApplicationField.NAME,
                ApplicationField.BIRTH_DATE,
                ApplicationField.GENDER,
                ApplicationField.EMAIL,
                ApplicationField.PHONE,
                ApplicationField.RESIDENCE,
            ),
            form.groups().first().second.map { it.field },
            "성별 and 거주지 belong to 기본 정보 (#242)",
        )
        assertEquals(8, form.fields.size, "a field the app cannot send is not shown")

        val kinds = form.fields.associate { it.field to it.kind }
        assertEquals(FieldKind.PHONE, kinds[ApplicationField.PHONE])
        assertEquals(FieldKind.DATE, kinds[ApplicationField.BIRTH_DATE])
        assertEquals(FieldKind.CHOICE, kinds[ApplicationField.GENDER])
        assertEquals(FieldKind.MULTILINE, kinds[ApplicationField.HISTORY])
    }

    @Test
    fun genderAndBaptismAreChoicesEvenWithoutServerOptions() {
        // The live application.hanmaum.de deployment sends no options at all.
        val form = ApplicationForms.open(course(field("gender", true), field("baptized"), field("baptizeType", true)), null)

        val byField = form.fields.associateBy { it.field }
        assertEquals(FieldKind.CHOICE, byField.getValue(ApplicationField.GENDER).kind)
        assertEquals(listOf("F", "M"), byField.getValue(ApplicationField.GENDER).options.map { it.value })
        assertEquals(listOf("1", "2", "3", "4"), byField.getValue(ApplicationField.BAPTIZED).options.map { it.value })
        assertEquals(listOf("1", "2", "3", "4", "5"), byField.getValue(ApplicationField.BAPTIZE_TYPE).options.map { it.value })
        assertEquals(FieldKind.CHOICE, byField.getValue(ApplicationField.BAPTIZE_TYPE).kind)
    }

    @Test
    fun optionsTheServerSendsWinOverTheKnownOnes() {
        val options = listOf(FormFieldOption("F", "여성"), FormFieldOption("M", "남성"), FormFieldOption("X", "기타"))

        val form = ApplicationForms.open(course(field("gender", true, options = options)), null)

        assertEquals(options, form.fields.single().options)
    }

    @Test
    fun nameAndBirthDateFromTheProfileAreLocked() {
        val form = ApplicationForms.open(course(field("name", true), field("birthDate", true), field("email", true)), prefill)

        val locked = form.fields.filter { it.locked }.map { it.field }
        assertEquals(listOf(ApplicationField.NAME, ApplicationField.BIRTH_DATE), locked)
        assertTrue(form.isLocked(ApplicationField.NAME))
        assertFalse(form.isLocked(ApplicationField.EMAIL))
    }

    @Test
    fun aBirthDateTheProfileLacksStaysEditable() {
        val form = ApplicationForms.open(course(field("name", true), field("birthDate", true)), prefill.copy(birthDate = null))

        assertTrue(form.isLocked(ApplicationField.NAME))
        assertFalse(form.isLocked(ApplicationField.BIRTH_DATE), "a locked empty field would make applying impossible")
        assertEquals(setOf(ApplicationField.BIRTH_DATE), form.missingFromProfile)
    }

    @Test
    fun lockedFieldsAreNeitherValidatedNorSent() {
        val form = ApplicationForms.open(course(field("name", true), field("birthDate", true), field("phone", true)), prefill)
            .copy(values = mapOf(ApplicationField.NAME to "김한마음", ApplicationField.BIRTH_DATE to "1995.03.14", ApplicationField.PHONE to "+49 170 1234567"))

        assertTrue(ApplicationForms.validate(form).isEmpty())
        assertEquals(mapOf(ApplicationField.PHONE to "+49 170 1234567"), ApplicationForms.requestValues(form))
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
    fun theMembersTrainingRecordsAndBaptismStartTheForm() {
        // Built by the server (hanmaum-dn-server#174); the app only puts them in.
        val withRecords = prefill.copy(
            history = "큐티베이직세미나 / 2017년 5월\n세례입교 / 2016년 11월",
            waiting = "일대일제자양육",
            running = "성경개관 구약",
            baptized = "3",
            baptizeType = "4",
        )

        val form = ApplicationForms.open(
            course(field("history"), field("waiting"), field("running"), field("baptized"), field("baptizeType", true)),
            withRecords,
        )

        assertEquals("큐티베이직세미나 / 2017년 5월\n세례입교 / 2016년 11월", form.values[ApplicationField.HISTORY])
        assertEquals("일대일제자양육", form.values[ApplicationField.WAITING])
        assertEquals("성경개관 구약", form.values[ApplicationField.RUNNING])
        assertEquals("3", form.values[ApplicationField.BAPTIZED])
        assertEquals("4", form.values[ApplicationField.BAPTIZE_TYPE])
        assertTrue(ApplicationForms.validate(form).isEmpty(), "a prefilled baptism code is a valid option")
        assertFalse(form.isLocked(ApplicationField.HISTORY), "the records stay editable")
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
        // No profile: nothing is locked, so name and birth date are sent as typed.
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
