package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.hanmaum.dn.mobile.core.presentation.theme.AppSize
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.theme.AppMotion
import com.hanmaum.dn.mobile.core.presentation.theme.AppSpacing

/**
 * The single screen scaffold. Every screen renders through this so chrome is consistent by
 * construction rather than by discipline.
 *
 * Owns, once, what each screen used to re-invent:
 *  - the collapsing large title (design spec §3 type scale, interpolated by Material3 between
 *    `headlineMedium` expanded and `titleLarge` collapsed — both already our Pretendard tokens)
 *  - the one back glyph: chevron-left in a 44dp target (§9)
 *  - page background, and separation-by-surface-shift when scrolled (§2 No-Line Rule)
 *
 * @param title screen title. Tab roots pass their nav label; detail screens their own title.
 * @param onBack `null` marks a tab root (no back affordance); non-null renders the chevron.
 * @param actions trailing icons in the bar, e.g. the Home notification bell.
 * @param content receives the scaffold's inner padding. Apply [AppSpacing.md] horizontally
 *   yourself — as `contentPadding` on lists, `Modifier.padding` on columns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    /** Compact Figma header for content-led detail screens such as the weekly bulletin. */
    compact: Boolean = false,
    /** Feature-provided ambience; Core does not choose a feature's glow palette. */
    glows: List<DnGlow> = emptyList(),
    content: @Composable (PaddingValues) -> Unit,
) {
    val strings = LocalStrings.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
        // §8 — every animation is spring(); never a tween or easing curve.
        snapAnimationSpec = AppMotion.screenPush,
    )

    val scaffold: @Composable () -> Unit = { Scaffold(
        // A fixed compact header must not consume scroll through the collapsing-bar behavior.
        modifier = if (compact) modifier else modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (compact) Color.Transparent else MaterialTheme.colorScheme.background,
        // The compact v2 branch below owns safe drawing. Keep Scaffold's own insets empty.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = snackbarHost,
        topBar = {
            if (compact) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        DnGlassIconButton(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, strings.back, onBack)
                    } else Box(Modifier.size(AppSize.touchTarget))
                    Text(
                        title,
                        modifier = Modifier.weight(1f).padding(horizontal = AppSpacing.sm),
                        style = DnTheme.typography.title,
                        color = DnTheme.colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                }
            } else LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = strings.back,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                },
                actions = actions,
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    // Scrolled state separates from content by a surface shift, not a divider.
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.outline,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        content = content,
    ) }
    if (compact) {
        // The v2 NavHost has no inset wrapper: the shared scaffold owns safe drawing here.
        DnBackground(glows = glows, insetContent = true) { scaffold() }
    } else scaffold()
}

/**
 * Eyebrow label + heading, the section-header pattern several screens previously hand-rolled
 * with differing type and spacing. Eyebrow uses the §3 `label` style — UPPERCASE, +2sp tracking.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
) {
    Column(modifier = modifier) {
        if (eyebrow != null) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(AppSpacing.xs))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
