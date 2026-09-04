package com.hanmaum.dn.mobile.features.login.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.login.domain.model.PasswordCriteria

/**
 * The password rules, ticked off as they are met.
 *
 * `PasswordPolicy` has always computed these — its own comment says the UI
 * "checklist never shows a green check for a rule the backend would reject" —
 * but the v2 screen rendered none of them, so the only feedback was a single
 * error after a failed submit. Showing the rules while typing turns a rejection
 * into guidance.
 *
 * A met rule is marked by both a different glyph and a different colour, never
 * colour alone.
 */
@Composable
fun PasswordCriteriaList(
    criteria: PasswordCriteria,
    modifier: Modifier = Modifier,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val shape = RoundedCornerShape(14.dp)

    val rules = listOf(
        strings.passwordRuleLength to criteria.minLength,
        strings.passwordRuleCase to criteria.hasUpperAndLower,
        strings.passwordRuleDigit to criteria.hasDigit,
        strings.passwordRuleSpecial to criteria.hasSpecial,
        strings.passwordRuleNotEmail to criteria.notEmail,
    )

    Column(
        modifier
            .clip(shape)
            .background(c.surface2, shape)
            .border(1.dp, c.strokeSubtle, shape)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rules.forEach { (label, met) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (met) DnIcons.Check else DnIcons.X,
                    contentDescription = null,
                    tint = if (met) c.lime else c.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = label,
                    style = DnTheme.typography.caption,
                    color = if (met) c.lime else c.textTertiary,
                )
            }
        }
    }
}
