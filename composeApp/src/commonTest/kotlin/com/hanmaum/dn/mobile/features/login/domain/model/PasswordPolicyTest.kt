package com.hanmaum.dn.mobile.features.login.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordPolicyTest {

    @Test
    fun emptyPasswordMeetsNoCriteria() {
        val c = PasswordPolicy.evaluate("", "user@example.com")
        assertFalse(c.minLength)
        assertFalse(c.hasUpperAndLower)
        assertFalse(c.hasDigit)
        assertFalse(c.hasSpecial)
        assertFalse(c.notEmail)
        assertFalse(c.allMet)
    }

    @Test
    fun fullyCompliantPasswordMeetsEveryCriterion() {
        val c = PasswordPolicy.evaluate("Secret123!", "user@example.com")
        assertTrue(c.minLength)
        assertTrue(c.hasUpperAndLower)
        assertTrue(c.hasDigit)
        assertTrue(c.hasSpecial)
        assertTrue(c.notEmail)
        assertTrue(c.allMet)
    }

    @Test
    fun shortPasswordFailsLength() {
        val c = PasswordPolicy.evaluate("Ab1!", "user@example.com")
        assertFalse(c.minLength)
        assertFalse(c.allMet)
    }

    @Test
    fun requiresBothUpperAndLowerCase() {
        assertFalse(PasswordPolicy.evaluate("SECRET123!", "u@e.com").hasUpperAndLower)
        assertFalse(PasswordPolicy.evaluate("secret123!", "u@e.com").hasUpperAndLower)
        assertTrue(PasswordPolicy.evaluate("Secret123!", "u@e.com").hasUpperAndLower)
    }

    @Test
    fun missingDigitOrSpecialFailsRespectiveRule() {
        assertFalse(PasswordPolicy.evaluate("Secretttt!", "u@e.com").hasDigit)
        assertFalse(PasswordPolicy.evaluate("Secret1234", "u@e.com").hasSpecial)
    }

    @Test
    fun whitespaceIsNotASpecialCharacter() {
        assertFalse(PasswordPolicy.evaluate("Secret 123", "u@e.com").hasSpecial)
    }

    @Test
    fun passwordEqualToEmailFailsNotEmailCaseInsensitively() {
        assertFalse(PasswordPolicy.evaluate("User@Example.com", "user@example.com").notEmail)
        assertTrue(PasswordPolicy.evaluate("Secret123!", "user@example.com").notEmail)
    }
}
