package com.hanmaum.dn.mobile.features.pending.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlow
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/** Where a member writes when they want to ask about the decision. */
private const val ADMIN_EMAIL = "hanmaum.dev@gmail.com"

/**
 * Shown to a member whose registration was refused.
 *
 * A dead end on purpose: no back, no logout, no retry. The session is left
 * intact so the splash lands here again on the next launch instead of dropping
 * the member at the login screen, where they could simply register once more.
 *
 * It cannot *prevent* that — reinstalling clears everything local. The real
 * guard belongs on the server (hanmaum-dn-server#139); this screen's job is to
 * say plainly what happened and where to ask, instead of the "please wait"
 * that a refused member used to be shown.
 */
@Composable
fun RejectedScreen() {
    val strings = LocalStrings.current
    val c = DnTheme.colors
    val uriHandler = LocalUriHandler.current

    DnBackground(
        glows = listOf(
            DnGlow(c.red, 0.75f, -0.1f, 1.1f, 0.10f),
            DnGlow(c.textTertiary, -0.2f, 0.6f, 0.9f, 0.05f),
        ),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(100.dp).clip(CircleShape).background(c.redDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.X, null, tint = c.red, modifier = Modifier.size(46.dp))
            }

            Spacer(Modifier.height(24.dp))
            Text(
                strings.rejectedTitle,
                style = DnTheme.typography.titleLg,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))
            Text(
                strings.rejectedBody,
                style = DnTheme.typography.body,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
            Text(
                strings.rejectedContactLabel,
                style = DnTheme.typography.caption,
                color = c.textTertiary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                ADMIN_EMAIL,
                style = DnTheme.typography.captionStrong,
                color = c.red,
                modifier = Modifier.clickable { uriHandler.openUri("mailto:$ADMIN_EMAIL") },
            )
        }
    }
}
