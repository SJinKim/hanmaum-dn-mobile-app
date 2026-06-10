package com.hanmaum.dn.mobile.features.login.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegisterValidationTest {

    @Test
    fun acceptsWellFormedEmails() {
        assertTrue(RegisterValidation.isValidEmail("hello@dnapp.com"))
        assertTrue(RegisterValidation.isValidEmail("a.b-c+tag@sub.example.co.kr"))
        assertTrue(RegisterValidation.isValidEmail("  trimmed@example.com  "))
    }

    @Test
    fun rejectsMalformedEmails() {
        assertFalse(RegisterValidation.isValidEmail(""))
        assertFalse(RegisterValidation.isValidEmail("plainstring"))
        assertFalse(RegisterValidation.isValidEmail("missing@dot"))
        assertFalse(RegisterValidation.isValidEmail("@no-local.com"))
        assertFalse(RegisterValidation.isValidEmail("no-domain@"))
        assertFalse(RegisterValidation.isValidEmail("has space@example.com"))
        assertFalse(RegisterValidation.isValidEmail("two@@example.com"))
    }

    @Test
    fun germanPostcodeIsExactlyFiveDigits() {
        assertTrue(RegisterValidation.isValidPostalCode("10115"))
        assertTrue(RegisterValidation.isValidPostalCode("01067")) // leading zero valid
        assertTrue(RegisterValidation.isValidPostalCode(" 10115 ")) // trimmed
        assertFalse(RegisterValidation.isValidPostalCode("1011"))
        assertFalse(RegisterValidation.isValidPostalCode("101150"))
        assertFalse(RegisterValidation.isValidPostalCode("ABCDE"))
    }

    @Test
    fun postcodeRespectsCountryFormat() {
        assertTrue(RegisterValidation.isValidPostalCode("1010", "AT"))
        assertFalse(RegisterValidation.isValidPostalCode("10115", "AT"))
        assertTrue(RegisterValidation.isValidPostalCode("90210-1234", "US"))
        // Unknown country falls back to a lenient shape.
        assertTrue(RegisterValidation.isValidPostalCode("ABC123", "ZZ"))
    }

    @Test
    fun cityMustContainLetters() {
        assertTrue(RegisterValidation.isValidCity("Berlin"))
        assertTrue(RegisterValidation.isValidCity("서울"))
        assertFalse(RegisterValidation.isValidCity("12345"))
        assertFalse(RegisterValidation.isValidCity("  "))
    }

    @Test
    fun houseNumberMustContainADigit() {
        assertTrue(RegisterValidation.isValidHouseNumber("5"))
        assertTrue(RegisterValidation.isValidHouseNumber("12a")) // letters allowed alongside digits
        assertTrue(RegisterValidation.isValidHouseNumber("12-14"))
        assertFalse(RegisterValidation.isValidHouseNumber(""))
        assertFalse(RegisterValidation.isValidHouseNumber("abc"))
    }
}
