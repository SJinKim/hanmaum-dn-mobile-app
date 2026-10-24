package com.hanmaum.dn.mobile.features.consent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnInnerShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.login.presentation.components.DEV_TEAM_EMAIL

/**
 * Where "개인정보 처리방침 보기" leads. Blank until the church publishes its
 * policy: the link stays hidden rather than opening a page that does not exist.
 */
internal const val PRIVACY_POLICY_URL = ""

/**
 * The APP_PRIVACY checkbox (#290). Never preselected: GDPR consent has to be
 * an act of the member, and a ticked box is no act at all.
 *
 * Box, label and detail share one 44pt-high toggle target; the policy link
 * sits outside it, so opening the policy never also ticks the box.
 */
@Composable
fun PrivacyConsentCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(DnInnerShape)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.TopStart) {
            val box = RoundedCornerShape(6.dp)
            Box(
                Modifier
                    .padding(top = 1.dp)
                    .size(22.dp)
                    .clip(box)
                    .background(if (checked) c.lime else c.surface2, box)
                    .then(if (checked) Modifier else Modifier.border(1.5.dp, c.strokeStrong, box)),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) Icon(DnIcons.Check, null, tint = c.onLime, modifier = Modifier.size(16.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(strings.consentCheckboxLabel, style = DnTheme.typography.bodyStrong, color = c.textPrimary)
            Text(strings.consentCheckboxDetail, style = DnTheme.typography.caption, color = c.textSecondary)
        }
    }
}

enum class ConsentNoticeTone { Neutral, Error, Amber }

/** Title-and-body notice: the granted record, a load failure, a version change. */
@Composable
fun ConsentNotice(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: ConsentNoticeTone = ConsentNoticeTone.Neutral,
) {
    val c = DnTheme.colors
    val (container, titleColor, bodyColor) = when (tone) {
        ConsentNoticeTone.Neutral -> Triple(c.surface, c.textPrimary, c.textSecondary)
        ConsentNoticeTone.Error -> Triple(c.redDim, c.red, c.red)
        ConsentNoticeTone.Amber -> Triple(c.amberDim, c.amber, c.amber)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(DnInnerShape)
            .background(container, DnInnerShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = DnTheme.typography.captionStrong, color = titleColor)
        Text(body, style = DnTheme.typography.caption, color = bodyColor)
    }
}

/** A 44pt-high text action. Lime-ink by default; red for the withdraw link. */
@Composable
fun ConsentLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = DnTheme.colors.limeInk,
) {
    Box(
        modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = DnTheme.typography.captionStrong, color = color)
    }
}

/** Privacy policy and privacy contact, side by side. */
@Composable
fun ConsentLinks(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        if (PRIVACY_POLICY_URL.isNotBlank()) {
            ConsentLink(strings.consentPolicyLink, { runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) } })
        }
        // Only opens the mail app — nothing is sent on the member's behalf.
        ConsentLink(strings.consentContactLink, { runCatching { uriHandler.openUri("mailto:$DEV_TEAM_EMAIL") } })
    }
}

/**
 * 11 — confirms a withdrawal. Withdrawing locks the app until the member
 * agrees again, so it is never a single tap.
 */
@Composable
fun WithdrawConsentDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isWorking: Boolean = false,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Dialog(onDismissRequest = { if (!isWorking) onDismiss() }) {
        Column(
            Modifier
                .width(305.dp)
                .clip(DnCardShape)
                .background(c.surface, DnCardShape)
                .border(1.dp, c.strokeSubtle, DnCardShape)
                .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val iconShape = RoundedCornerShape(16.dp)
            Box(
                Modifier.size(48.dp).clip(iconShape).background(c.redDim, iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.AlertTriangle, null, tint = c.red, modifier = Modifier.size(22.dp))
            }
            Text(
                strings.consentWithdrawTitle,
                style = DnTheme.typography.headline,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                strings.consentWithdrawBody,
                style = DnTheme.typography.caption,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogPill(strings.cancel, c.surface2, c.textPrimary, !isWorking, onDismiss, Modifier.weight(1f))
                DialogPill(strings.consentWithdrawConfirm, c.red, c.onRed, !isWorking, onConfirm, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DialogPill(
    label: String,
    container: Color,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .clip(shape)
            .background(container, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = DnTheme.typography.bodyStrong, color = content)
    }
}
