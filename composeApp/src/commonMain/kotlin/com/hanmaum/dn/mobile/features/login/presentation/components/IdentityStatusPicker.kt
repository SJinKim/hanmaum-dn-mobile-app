package com.hanmaum.dn.mobile.features.login.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnInnerShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.login.domain.model.IdentityStatus

/** The label a member reads for each 신분 — the server name never reaches the screen. */
internal fun AppStrings.labelFor(status: IdentityStatus): String = when (status) {
    IdentityStatus.EMPLOYEE -> identityEmployee
    IdentityStatus.UNIVERSITY_STUDENT -> identityUniversityStudent
    IdentityStatus.EXAM_PREPARATION -> identityExamPreparation
    IdentityStatus.WORKING_HOLIDAY -> identityWorkingHoliday
    IdentityStatus.EXCHANGE_STUDENT -> identityExchangeStudent
    IdentityStatus.SELF_EMPLOYED -> identitySelfEmployed
    IdentityStatus.EXPATRIATE -> identityExpatriate
    IdentityStatus.JOB_SEEKING -> identityJobSeeking
}

/**
 * The 신분 field (#282): looks like a text field, but a tap opens
 * [IdentityStatusPickerDialog] instead of a keyboard. Focusable so the form
 * can move focus here when it is the first field left empty.
 */
@Composable
internal fun IdentityStatusField(
    label: String,
    value: IdentityStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val shape = RoundedCornerShape(18.dp)
    Column(modifier) {
        Text(label, style = DnTheme.typography.label, color = c.textTertiary)
        Spacer(Modifier.size(7.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.surface2, shape)
                // Same outline rule as DnTextField: red and thicker on error,
                // always paired with the message underneath.
                .border(
                    width = if (isError) 1.5.dp else 1.dp,
                    color = if (isError) c.red else c.strokeSubtle,
                    shape = shape,
                )
                .pressClick(onClick)
                .focusable()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                value?.let { strings.labelFor(it) } ?: strings.identityStatusPlaceholder,
                style = DnTheme.typography.captionStrong,
                color = if (value != null) c.textPrimary else c.textTertiary,
                modifier = Modifier.weight(1f),
            )
            Icon(DnIcons.ChevronUp, null, tint = c.textTertiary, modifier = Modifier.size(16.dp).rotate(180f))
        }
    }
}

/**
 * 신분 선택 — the eight options in the web app's order. Tapping a row
 * applies it and closes the dialog, like the country picker beside it.
 */
@Composable
internal fun IdentityStatusPickerDialog(
    selected: IdentityStatus?,
    onSelect: (IdentityStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(DnCardShape)
                .background(c.surface, DnCardShape)
                .border(1.dp, c.strokeSubtle, DnCardShape)
                .padding(horizontal = 18.dp, vertical = 22.dp),
        ) {
            Text(strings.identityStatusPickerTitle, style = DnTheme.typography.title, color = c.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(strings.identityStatusPickerHint, style = DnTheme.typography.caption, color = c.textSecondary)
            Spacer(Modifier.height(14.dp))
            IdentityStatus.entries.forEach { status ->
                val isSelected = status == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(DnInnerShape)
                        .background(if (isSelected) c.surface2 else c.surface)
                        .pressClick { onSelect(status) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        strings.labelFor(status),
                        style = if (isSelected) DnTheme.typography.bodyStrong else DnTheme.typography.body,
                        color = c.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(DnIcons.Check, null, tint = c.limeInk, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                strings.phoneCountryTapToApply,
                style = DnTheme.typography.caption,
                color = c.textTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
