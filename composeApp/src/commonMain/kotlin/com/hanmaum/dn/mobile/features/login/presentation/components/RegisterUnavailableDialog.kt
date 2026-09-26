package com.hanmaum.dn.mobile.features.login.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography

private const val DEV_TEAM_EMAIL = "hanmaum.dev@gmail.com"

/**
 * 가입 신청 불가 (#156): the backend failed on its side, so the form cannot be
 * sent right now whatever the member types.
 *
 * The team name is a mailto link that only opens the mail app — nothing is sent
 * and no error detail goes into it. The address is also printed, selectable,
 * for a device without a mail app, where opening the link is caught instead of
 * crashing.
 */
@Composable
internal fun RegisterUnavailableDialog(onDismiss: () -> Unit) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current

    val contact = buildAnnotatedString {
        append(strings.registerUnavailableContactPrefix)
        withLink(
            LinkAnnotation.Clickable(
                tag = "mailto",
                styles = TextLinkStyles(
                    SpanStyle(color = c.red, textDecoration = TextDecoration.Underline),
                ),
            ) { runCatching { uriHandler.openUri("mailto:$DEV_TEAM_EMAIL") } },
        ) { append(strings.registerUnavailableTeam) }
        append(strings.registerUnavailableContactSuffix)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(DnCardShape)
                .background(c.surface, DnCardShape)
                .border(1.dp, c.strokeSubtle, DnCardShape)
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(DnTileShape)
                    .background(c.redDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.AlertTriangle, null, tint = c.red, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                strings.registerUnavailableTitle,
                style = DnTheme.typography.headline,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                strings.registerUnavailableBody,
                style = DnTheme.typography.caption,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                contact,
                style = DnTheme.typography.caption,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            SelectionContainer {
                Text(
                    DEV_TEAM_EMAIL,
                    style = DnTheme.typography.captionStrong,
                    color = c.textPrimary,
                )
            }
            Spacer(Modifier.height(18.dp))
            DnPrimaryButton(
                label = strings.confirm,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
