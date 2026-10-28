package com.hanmaum.dn.mobile.core.i18n

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class LocalizationCoverageTest {
    @Test fun allStringPropertiesHaveBothTranslations() {
        AppStrings::class.java.methods.filter { it.parameterCount == 0 && it.returnType == String::class.java }
            .forEach { getter ->
                listOf(EnStrings, KoStrings).forEach { strings ->
                    val value = getter.invoke(strings) as String
                    assertTrue(value.isNotBlank() || getter.name == "getYearSuffix", "${getter.name}: blank in ${strings.languageTag}")
                    val properNamesAndExamples = setOf("getChurchName", "getRegisterLastNameExample", "getRegisterFirstNameExample")
                    if (strings === EnStrings && getter.name !in properNamesAndExamples) {
                        assertTrue(!Regex("[가-힣]").containsMatchIn(value), "${getter.name}: Korean UI in English")
                    }
                }
            }
    }

    @Test fun koreanUiCopyLivesInTheLocalizationLayer() {
        // Platform sets too: notification copy posted from androidMain/iosMain reaches the user as well.
        val root = listOf(File("src"), File("composeApp/src")).first { File(it, "commonMain/kotlin").isDirectory }
        val sources = listOf("commonMain", "androidMain", "iosMain").map { File(root, "$it/kotlin") }
        val comments = Regex("""/\*.*?\*/|(?m)^\s*//[^\n]*$""", RegexOption.DOT_MATCHES_ALL)
        val koreanLiteral = Regex("\"[^\"\n]*[가-힣][^\"\n]*\"")
        val findings = sources.asSequence().flatMap { it.walkTopDown() }.filter { it.extension == "kt" && "i18n" !in it.toPath().map { part -> part.toString() } }
            .flatMap { file ->
                koreanLiteral.findAll(file.readText().replace(comments, ""))
                    .map { "${file.name}: ${it.value}" }
            }.toList()
        assertTrue(findings.isEmpty(), "Move app-owned copy to AppStrings/UiMessage:\n${findings.joinToString("\n")}")
    }
}
