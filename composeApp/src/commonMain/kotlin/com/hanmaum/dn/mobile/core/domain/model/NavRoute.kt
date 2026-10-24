package com.hanmaum.dn.mobile.core.domain.model

enum class NavRoute {
    Home, PendingApproval, Rejected, Login,

    /** APP_PRIVACY is missing or outdated; nothing else opens until it is answered (#290). */
    Consent,
}