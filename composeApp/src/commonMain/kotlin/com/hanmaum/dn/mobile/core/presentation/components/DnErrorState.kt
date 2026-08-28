package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnPillShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/**
 * Shown wherever a screen's data could not be fetched.
 *
 * A raw exception string tells the reader nothing they can act on, so the
 * failure is stated plainly and paired with the one thing that usually fixes
 * it. Red carries the failure, but the retry button keeps the lime action
 * role — retrying is the action, not the error.
 */
@Composable
fun DnErrorState(
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onGoHome: (() -> Unit)? = null,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(c.redDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DnIcons.AlertTriangle, null, tint = c.red, modifier = Modifier.size(46.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            strings.errorTitle,
            style = DnTheme.typography.titleLg,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            strings.errorBody,
            style = DnTheme.typography.body,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )

        if (onRetry != null) {
            Spacer(Modifier.height(28.dp))
            DnPrimaryButton(
                label = strings.errorRetry,
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (onGoHome != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .clip(DnPillShape)
                    .clickable(onClick = onGoHome)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(DnIcons.Home, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                Text(strings.errorGoHome, style = DnTheme.typography.captionStrong, color = c.textSecondary)
            }
        }
    }
}
