package com.hanmaum.dn.mobile.features.bulletin

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.bulletin.data.model.BulletinResponse
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

internal val bulletinJson = """
    {"publicId":"edition-1","serviceDate":"2026-10-11","volume":41,"status":"PUBLISHED",
     "servicePublicId":"service-3","serviceName":"3부 예배","serviceStartTime":"14:00:00",
     "openingPrayerBy":"Opening prayer","offeringSongBy":"Offering singer",
     "scriptureReference":"John 21:15-17","sermonTitle":"Begin again","sermonPreacher":"Preacher",
     "responsePrayerBy":"Response prayer","responseSong":"Response song",
     "songs":["First song","Second song"],
     "announcements":[{"title":"First notice","body":"Longer body"},{"title":"Second notice","body":null}],
     "sharingBlocks":[{"type":"HEADING","text":"Heading"},{"type":"PARAGRAPH","text":"Paragraph"},
       {"type":"SCRIPTURE","text":"Scripture text","reference":"John 21:16"},{"type":"QUESTION","text":"First question"}],
     "sectionTitles":[{"key":"SECTION_WORSHIP","title":"Custom worship title","defaultTitle":"경배와 찬양"},
       {"key":"SECTION_OFFERING","title":"Offering"},{"key":"SECTION_SENDING","title":"Sending"},
       {"key":"FIXED_BLESSING_PRAYER","title":"Blessing prayer"}],
     "publishedAt":"2026-10-10T10:00:00Z","withdrawnAt":null,"version":7}
""".trimIndent()

internal fun bulletin() = Json.decodeFromString<BulletinResponse>(bulletinJson).toDomain()

internal class BulletinTokens(subject: String = "member-1") : TokenStorage {
    var subject: String? = subject
    override fun getAccessToken(): String? = subject?.let {
        "header.${Base64.UrlSafe.encode("""{"sub":"$it","iss":"test-realm"}""".encodeToByteArray()).trimEnd('=')}.signature"
    }
    override fun saveAccessToken(token: String) = Unit
    override fun getRefreshToken(): String? = null
    override fun saveRefreshToken(token: String?) = Unit
    override fun clear() { subject = null }
}
