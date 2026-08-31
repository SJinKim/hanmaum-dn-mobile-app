package com.hanmaum.dn.mobile.core.domain.model

import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MemberStatusTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun member(status: String) = """
        {"publicId":"p1","firstName":"서진","lastName":"김","status":"$status"}
    """.trimIndent()

    @Test
    fun knownWireValuesMapToTheirStatus() {
        assertEquals(MemberStatus.ACTIVE, MemberStatus.fromWire("ACTIVE"))
        assertEquals(MemberStatus.PENDING, MemberStatus.fromWire("PENDING"))
        assertEquals(MemberStatus.DELETED, MemberStatus.fromWire("DELETED"))
        assertEquals(MemberStatus.INACTIVE, MemberStatus.fromWire("INACTIVE"))
    }

    @Test
    fun theHistoricalRejectedValueIsAccepted() {
        // Deprecated server-side but still present on old rows. Before this it
        // failed the whole /members/me response.
        assertEquals(MemberStatus.REJECTED, json.decodeFromString<MemberResponse>(member("REJECTED")).status)
    }

    @Test
    fun aStatusThisBuildDoesNotKnowDeserialisesAsUnknown() {
        assertEquals(MemberStatus.UNKNOWN, json.decodeFromString<MemberResponse>(member("GRADUATED")).status)
    }

    @Test
    fun anUnknownStatusDoesNotCostTheRestOfTheResponse() {
        val member = json.decodeFromString<MemberResponse>(member("SOMETHING_NEW"))
        assertEquals("p1", member.publicId)
        assertEquals("김", member.lastName)
    }

    @Test
    fun aKnownStatusStillRoundTrips() {
        val encoded = json.encodeToString(MemberStatus.ACTIVE)
        assertEquals("\"ACTIVE\"", encoded)
        assertEquals(MemberStatus.ACTIVE, json.decodeFromString<MemberStatus>(encoded))
    }
}
