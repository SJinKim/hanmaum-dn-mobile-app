package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.glass.dnGlass
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnPillShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/** Circular glass button used for back, overflow and the notification bell. */
@Composable
fun DnGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 50))
            .dnGlass(shape = RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = DnTheme.colors.textPrimary,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

/**
 * Top bar: back on the left, title centred, one action on the right.
 * Present on every screen except Home, which has its own greeting header.
 */
@Composable
fun DnTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = DnIcons.More,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            DnGlassIconButton(DnIcons.ArrowLeft, "뒤로", onBack)
        } else {
            Box(Modifier.size(44.dp))
        }
        Text(
            text = title,
            style = DnTheme.typography.title,
            color = DnTheme.colors.textPrimary,
        )
        if (actionIcon != null && onAction != null) {
            DnGlassIconButton(actionIcon, actionDescription, onAction)
        } else {
            Box(Modifier.size(44.dp))
        }
    }
}

/**
 * Filter and navigation chip. The selected state is high-contrast neutral,
 * not the accent — a selected filter is a state, not an action, and the
 * accent budget is reserved for things that do something.
 */
@Composable
fun DnChip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = DnTheme.colors
    Row(
        modifier
            .clip(DnPillShape)
            .background(if (selected) c.inverse else c.surface2, DnPillShape)
            .border(1.dp, if (selected) c.inverse else c.strokeSubtle, DnPillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = label,
            style = DnTheme.typography.captionStrong,
            color = if (selected) c.textInverse else c.textSecondary,
        )
        trailing?.invoke()
    }
}

/** Two-state segmented control, used for peer lists that share a layout. */
@Composable
fun DnSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    counts: List<String?> = emptyList(),
) {
    val c = DnTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface2, RoundedCornerShape(24.dp))
            .border(1.dp, c.strokeSubtle, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) c.inverse else Color.Transparent, RoundedCornerShape(20.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = DnTheme.typography.captionStrong,
                    color = if (on) c.textInverse else c.textSecondary,
                )
                counts.getOrNull(i)?.let {
                    Text(
                        text = it,
                        style = DnTheme.typography.label,
                        color = if (on) c.textInverse else c.textTertiary,
                    )
                }
            }
        }
    }
}

/**
 * Stand-in for imagery the backend does not deliver yet.
 *
 * Deliberately framed and dashed rather than a flat block: it reserves the
 * real amount of space so the layout can be judged, while staying obviously
 * unfinished so nobody mistakes it for a design decision.
 */
@Composable
fun DnImagePlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    label: String? = null,
) {
    val c = DnTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier
            .clip(shape)
            .background(c.surface2, shape)
            .drawBehind {
                drawRoundRect(
                    color = c.strokeStrong,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                        ),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = DnIcons.Image,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(24.dp),
            )
            if (label != null) {
                Text(label, style = DnTheme.typography.label, color = c.textTertiary)
            }
        }
    }
}

/** Primary action. The only place the lime fill is allowed to dominate. */
@Composable
fun DnPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
) {
    val c = DnTheme.colors
    Row(
        modifier
            .clip(DnPillShape)
            .background(if (enabled) c.lime else c.surface3, DnPillShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            Icon(it, null, tint = if (enabled) c.onLime else c.textTertiary, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            style = DnTheme.typography.bodyStrong,
            color = if (enabled) c.onLime else c.textTertiary,
        )
    }
}

/** Secondary / destructive action, tinted rather than solid so it yields. */
@Composable
fun DnTintedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = DnTheme.colors.red,
    container: Color = DnTheme.colors.redDim,
) {
    Row(
        modifier
            .clip(DnPillShape)
            .background(container, DnPillShape)
            .border(1.dp, tint, DnPillShape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = DnTheme.typography.bodyStrong, color = tint)
    }
}

/** Convenience for rows that need a flexible gap. */
@Composable
fun RowScope.DnSpacer() = Box(Modifier.weight(1f))

/** Labelled text field used across the auth and profile forms. */
@Composable
fun DnTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    isPassword: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType =
        androidx.compose.ui.text.input.KeyboardType.Text,
) {
    val c = DnTheme.colors
    // A password field owns its own reveal state: every caller wants the same
    // behaviour, and leaving it to them produced an eye icon that did nothing.
    var revealed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.foundation.layout.Column(modifier) {
        Text(label, style = DnTheme.typography.label, color = c.textTertiary)
        androidx.compose.foundation.layout.Spacer(Modifier.size(7.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.surface2, RoundedCornerShape(18.dp))
                .border(1.dp, c.strokeSubtle, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            leading?.let { Icon(it, null, tint = c.textTertiary, modifier = Modifier.size(18.dp)) }
            Box(Modifier.weight(1f)) {
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = DnTheme.typography.captionStrong.copy(color = c.textPrimary),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(c.lime),
                    visualTransformation = if (isPassword && !revealed) {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    } else {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = keyboardType,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = DnTheme.typography.captionStrong, color = c.textTertiary)
                }
            }
            // The icon itself stays 18 dp, but the tap target around it is 44 dp
            // so it clears the minimum touch size. The target grows inward, so a
            // small outward offset keeps the glyph optically where it was — an
            // offset, not padding, because Compose rejects negative padding.
            val onTrailing = when {
                isPassword -> ({ revealed = !revealed })
                else -> onTrailingClick
            }
            trailing?.let {
                val glyph = if (isPassword && revealed) DnIcons.EyeOff else it
                Box(
                    Modifier
                        .offset(x = 6.dp)
                        .size(44.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .then(
                            if (onTrailing != null) {
                                Modifier.clickable(onClick = onTrailing)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        glyph,
                        contentDescription = if (isPassword) {
                            if (revealed) "비밀번호 숨기기" else "비밀번호 표시"
                        } else {
                            null
                        },
                        tint = if (isPassword && revealed) c.textPrimary else c.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
