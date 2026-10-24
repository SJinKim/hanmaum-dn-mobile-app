package com.hanmaum.dn.mobile.features.login.domain.model

import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentGrant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RegisterRequestTest {

    @Test
    fun serializesAddressUsingBackendContractAndPreservesKoreanNames() {
        val encoded = Json.encodeToString(
            RegisterRequest.serializer(),
            RegisterRequest(
                firstName = "승진",
                lastName = "김",
                email = "seungjin@example.com",
                city = "Berlin",
                password = "Secret123!",
                birthDate = "2000-08-16",
                identityStatus = "UNIVERSITY_STUDENT",
                street = "Hauptstraße",
                houseNumber = "12a",
                zipCode = "10115",
                consent = ConsentGrant(accepted = true, version = "2026-09"),
            )
        )
        val json = Json.parseToJsonElement(encoded).jsonObject

        assertEquals("승진", json.getValue("firstName").jsonPrimitive.content)
        assertEquals("김", json.getValue("lastName").jsonPrimitive.content)
        assertEquals("10115", json.getValue("zipCode").jsonPrimitive.content)
        assertFalse(json.containsKey("zip_code"))
        // Street and house number go out as separate camelCase fields — never combined.
        assertEquals("Hauptstraße", json.getValue("street").jsonPrimitive.content)
        assertEquals("12a", json.getValue("houseNumber").jsonPrimitive.content)
        // The consent travels as the server's ConsentGrant object (#290).
        val consent = json.getValue("consent").jsonObject
        assertEquals("true", consent.getValue("accepted").jsonPrimitive.content)
        assertEquals("2026-09", consent.getValue("version").jsonPrimitive.content)
    }
}
