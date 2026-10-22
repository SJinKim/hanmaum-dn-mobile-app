package com.hanmaum.dn.mobile.features.announcement.presentation

import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement

internal fun Announcement.categoryLabel(strings: AppStrings): String = when (category) {
    "MINISTRY" -> strings.ministryDetailTitle
    "NOTICE" -> strings.categoryNotice
    "EVENT" -> strings.categoryEvent
    else -> strings.notifications
}
