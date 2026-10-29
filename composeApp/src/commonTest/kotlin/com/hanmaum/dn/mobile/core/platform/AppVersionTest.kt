package com.hanmaum.dn.mobile.core.platform

import com.hanmaum.dn.mobile.core.i18n.EnStrings
import com.hanmaum.dn.mobile.core.i18n.KoStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppVersionTest {
    @Test
    fun usesInstalledMarketingVersionAndKeepsItsNumericReleaseLine() {
        assertEquals("0.10.0", displayAppVersion("0.10.0"))
        assertEquals("1.2.3", displayAppVersion("1.2.3-st.4+123"))
        assertEquals("1.2.3", displayAppVersion(" 1.2.3 "))
        assertEquals("0.0.0", displayAppVersion("0.0"))
    }

    @Test
    fun missingOrMalformedMetadataDoesNotInventAVersion() {
        for (value in listOf(null, "", " ", "unknown", "1", "v1.2.3", "1.2.3.4", "1.2.bad")) {
            assertNull(displayAppVersion(value), value)
        }
    }

    @Test
    fun loginVersionCaptionIsLocalised() {
        assertEquals("Version 1.2.3", EnStrings.appVersion("1.2.3"))
        assertEquals("버전 1.2.3", KoStrings.appVersion("1.2.3"))
    }
}
