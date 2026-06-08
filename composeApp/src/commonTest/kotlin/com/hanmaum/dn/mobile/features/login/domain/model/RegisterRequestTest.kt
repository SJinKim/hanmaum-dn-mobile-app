package com.hanmaum.dn.mobile.features.login.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RegisterRequestTest {

    @Test
    fun serializesZipCodeUsingBackendContractAndPreservesKoreanNames() {
        val encoded = Json.encodeToString(
            RegisterRequest.serializer(),
            RegisterRequest(
                firstName = "승진",
                lastName = "김",
                email = "seungjin@example.com",
                city = "Berlin",
                password = "Secret123!",
                zipCode = "10115",
            )
        )
        val json = Json.parseToJsonElement(encoded).jsonObject

        assertEquals("승진", json.getValue("firstName").jsonPrimitive.content)
        assertEquals("김", json.getValue("lastName").jsonPrimitive.content)
        assertEquals("10115", json.getValue("zipCode").jsonPrimitive.content)
        assertFalse(json.containsKey("zip_code"))
    }
}
