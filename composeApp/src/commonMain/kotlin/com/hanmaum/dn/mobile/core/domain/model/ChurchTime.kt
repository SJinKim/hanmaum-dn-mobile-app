package com.hanmaum.dn.mobile.core.domain.model

import kotlinx.datetime.TimeZone

/** Church publication/service dates use Berlin, matching the server's Sunday boundary. */
val ChurchTimeZone: TimeZone = TimeZone.of("Europe/Berlin")
