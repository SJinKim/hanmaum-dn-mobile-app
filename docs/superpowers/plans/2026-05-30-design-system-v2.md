# Design System v2 — Warm Premium Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the existing "Luminous Sanctuary" theme with the "Warm Premium" design system — Pretendard font, adaptive dark/light colour scheme, floating pill navigation, spring motion constants, and predictive back support.

**Architecture:** All theme tokens live in `core/presentation/theme/`. The floating pill nav replaces Scaffold's `bottomBar` — it is rendered as an overlay `Box` inside the NavHost's parent. Every existing screen picks up colour/shape/type changes automatically via `MaterialTheme` tokens; only the navigation host and top bar require structural edits.

**Tech Stack:** Compose Multiplatform 1.10, Material3, `navigation-compose`, Pretendard Variable OTF (static weight files), `androidx.compose.animation.core` spring APIs.

**Design reference:** `designs/dn_app/DESIGN.md` — consult it for every token value.

---

## File Map

| Action | File | Responsibility |
|---|---|---|
| Delete | `composeResources/font/PlusJakartaSans-*.ttf` (×5) | Removed — replaced by Pretendard |
| Add | `composeResources/font/Pretendard-Regular.otf` | Weight 400 |
| Add | `composeResources/font/Pretendard-SemiBold.otf` | Weight 600 |
| Add | `composeResources/font/Pretendard-Bold.otf` | Weight 700 |
| Add | `composeResources/font/Pretendard-ExtraBold.otf` | Weight 800 |
| Add | `composeResources/font/Pretendard-Black.otf` | Weight 900 |
| Rewrite | `core/presentation/theme/AppColors.kt` | Warm premium light + dark colour schemes |
| Rewrite | `core/presentation/theme/AppTypography.kt` | Pretendard, new type scale |
| Rewrite | `core/presentation/theme/AppShapes.kt` | New radius tokens (6 / 14 / 20 / ∞) |
| Rewrite | `core/presentation/theme/AppTheme.kt` | Adaptive dark/light, wires all tokens |
| Create | `core/presentation/theme/AppMotion.kt` | Spring spec constants |
| Rewrite | `core/presentation/components/FloatingPillNav.kt` | New pill nav (replaces BottomNavBar.kt) |
| Modify | `App.kt` | Overlay pill, remove Scaffold bottomBar, add bottom padding |
| Rewrite | `core/presentation/components/AppTopBar.kt` | Chevron back, muted colour, transparent bg |
| Modify | `androidMain/AndroidManifest.xml` | Enable predictive back |

---

## Task 1 — Download Pretendard Font Files

**Files:**
- Delete: `composeApp/src/commonMain/composeResources/font/PlusJakartaSans-*.ttf` (5 files)
- Add: 5 Pretendard OTF files in same directory

- [ ] **Step 1: Download Pretendard static OTF files**

Go to the Pretendard GitHub releases page and download the `Pretendard-1.3.9.zip` (or latest) from:
`https://github.com/orioncactus/pretendard/releases`

Unzip it. From the `public/static/` folder, copy these 5 files to
`composeApp/src/commonMain/composeResources/font/`:

```
Pretendard-Regular.otf    → weight 400
Pretendard-SemiBold.otf   → weight 600
Pretendard-Bold.otf       → weight 700
Pretendard-ExtraBold.otf  → weight 800
Pretendard-Black.otf      → weight 900
```

- [ ] **Step 2: Delete Plus Jakarta Sans font files**

```bash
rm composeApp/src/commonMain/composeResources/font/PlusJakartaSans-Regular.ttf
rm composeApp/src/commonMain/composeResources/font/PlusJakartaSans-Medium.ttf
rm composeApp/src/commonMain/composeResources/font/PlusJakartaSans-SemiBold.ttf
rm composeApp/src/commonMain/composeResources/font/PlusJakartaSans-Bold.ttf
rm composeApp/src/commonMain/composeResources/font/PlusJakartaSans-ExtraBold.ttf
```

- [ ] **Step 3: Verify font directory**

```bash
ls composeApp/src/commonMain/composeResources/font/
```

Expected output — exactly these 5 files:
```
Pretendard-Black.otf
Pretendard-Bold.otf
Pretendard-ExtraBold.otf
Pretendard-Regular.otf
Pretendard-SemiBold.otf
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/composeResources/font/
git commit -m "chore(fonts): replace Plus Jakarta Sans with Pretendard"
```

---

## Task 2 — Rewrite AppTypography.kt

**Files:**
- Rewrite: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTypography.kt`

- [ ] **Step 1: Rewrite AppTypography.kt**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.Pretendard_Regular
import hanmaumdnapp.composeapp.generated.resources.Pretendard_SemiBold
import hanmaumdnapp.composeapp.generated.resources.Pretendard_Bold
import hanmaumdnapp.composeapp.generated.resources.Pretendard_ExtraBold
import hanmaumdnapp.composeapp.generated.resources.Pretendard_Black
import org.jetbrains.compose.resources.Font

@Composable
fun rememberPretendard(): FontFamily = FontFamily(
    Font(Res.font.Pretendard_Regular,   FontWeight.Normal),
    Font(Res.font.Pretendard_SemiBold,  FontWeight.SemiBold),
    Font(Res.font.Pretendard_Bold,      FontWeight.Bold),
    Font(Res.font.Pretendard_ExtraBold, FontWeight.ExtraBold),
    Font(Res.font.Pretendard_Black,     FontWeight.Black),
)

@Composable
fun rememberAppTypography(): Typography {
    val ff = rememberPretendard()
    return remember(ff) {
        Typography(
            // Display — hero greetings, splash statements
            displayLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Black,     fontSize = 32.sp, lineHeight = 38.sp,  letterSpacing = (-1.2).sp),
            displayMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Black,     fontSize = 28.sp, lineHeight = 34.sp,  letterSpacing = (-1.0).sp),
            displaySmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp,  letterSpacing = (-0.8).sp),
            // Headline — section titles, card headlines
            headlineLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.8).sp),
            headlineMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.6).sp),
            headlineSmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold,      fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.4).sp),
            // Title — card titles, list row titles
            titleLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold,     fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.4).sp),
            titleMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp),
            titleSmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp),
            // Body — descriptions, detail content (Korean: use lineHeight 1.7× size)
            bodyLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.4.sp, letterSpacing = 0.sp),
            bodyMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 19.2.sp, letterSpacing = 0.sp),
            bodySmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 17.6.sp, letterSpacing = 0.sp),
            // Label — eyebrow tags, nav labels (apply .uppercase() at call site for eyebrow)
            labelLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 2.0.sp),
            labelMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold, fontSize = 9.sp,  lineHeight = 13.sp, letterSpacing = 1.5.sp),
            labelSmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 8.sp, lineHeight = 12.sp, letterSpacing = 1.0.sp),
        )
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew :composeApp:compileKotlinAndroid --daemon
```

Expected: `BUILD SUCCESSFUL` — the Res.font references auto-generate from the OTF file names (e.g. `Pretendard-Regular.otf` → `Res.font.Pretendard_Regular`). If the generated names differ, check `composeApp/build/generated/` for the actual resource identifiers.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTypography.kt
git commit -m "feat(theme): replace PJS with Pretendard, new type scale"
```

---

## Task 3 — Rewrite AppColors.kt

**Files:**
- Rewrite: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppColors.kt`

- [ ] **Step 1: Rewrite AppColors.kt**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Light Mode Surfaces ───────────────────────────────────────────────────────
val LightSurface            = Color(0xFFFDF8F4) // page background
val LightSurfaceContainerLow = Color(0xFFFAF3ED) // section background / shift separator
val LightSurfaceContainer   = Color(0xFFF5EBE0)  // grouped content background
val LightSurfaceContainerLowest = Color(0xFFFFFFFF) // cards / sheets
val LightOnSurface          = Color(0xFF2C1A0E)  // primary text
val LightOnSurfaceVariant   = Color(0xFF5A3A28)  // secondary text, descriptions
val LightMuted              = Color(0xFFC4A882)  // labels, placeholders, inactive icons
val LightOutlineVariant     = Color(0x26C4A882)  // ghost border (15 % opacity)

// ── Dark Mode Surfaces ────────────────────────────────────────────────────────
val DarkSurface             = Color(0xFF1A1208)
val DarkSurfaceContainerLow = Color(0xFF120D05)
val DarkSurfaceContainer    = Color(0xFF221508)
val DarkSurfaceContainerLowest = Color(0xFF0D0905)
val DarkOnSurface           = Color(0xFFF5E6CC)
val DarkOnSurfaceVariant    = Color(0xFFC4A070)
val DarkMuted               = Color(0xFF8A6A3A)
val DarkOutlineVariant      = Color(0x2E8A6A3A)  // 18 % opacity

// ── CI Placeholder — swap when brand colours are confirmed ────────────────────
// Light
val LightPrimary            = Color(0xFFC07A50)
val LightPrimaryDark        = Color(0xFF8A4A28)
val LightOnPrimary          = Color(0xFFFFFFFF)
// Dark
val DarkPrimary             = Color(0xFFA0622A)
val DarkPrimaryDark         = Color(0xFF6A3A10)
val DarkOnPrimary           = Color(0xFFFDE8C0)

// ── Shared ────────────────────────────────────────────────────────────────────
val ErrorRed         = Color(0xFFBA1A1A)
val ErrorContainer   = Color(0xFFFFDAD6)
val OnError          = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF410002)

// ── Material3 colour schemes ──────────────────────────────────────────────────
val WarmPremiumLightColorScheme = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = LightOnPrimary,
    primaryContainer     = LightPrimaryDark,
    onPrimaryContainer   = LightOnPrimary,
    secondary            = LightMuted,
    onSecondary          = LightOnSurface,
    secondaryContainer   = LightSurfaceContainerLow,
    onSecondaryContainer = LightOnSurfaceVariant,
    background           = LightSurface,
    onBackground         = LightOnSurface,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceContainerLow,
    onSurfaceVariant     = LightOnSurfaceVariant,
    surfaceContainer            = LightSurfaceContainer,
    surfaceContainerLow         = LightSurfaceContainerLow,
    surfaceContainerLowest      = LightSurfaceContainerLowest,
    outline              = LightMuted,
    outlineVariant       = LightOutlineVariant,
    error                = ErrorRed,
    onError              = OnError,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    scrim                = Color(0xFF2C1A0E),
    inverseSurface       = DarkSurface,
    inverseOnSurface     = DarkOnSurface,
    inversePrimary       = DarkPrimary,
)

val WarmPremiumDarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = DarkPrimaryDark,
    onPrimaryContainer   = DarkOnPrimary,
    secondary            = DarkMuted,
    onSecondary          = DarkOnSurface,
    secondaryContainer   = DarkSurfaceContainerLow,
    onSecondaryContainer = DarkOnSurfaceVariant,
    background           = DarkSurface,
    onBackground         = DarkOnSurface,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceContainerLow,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    surfaceContainer            = DarkSurfaceContainer,
    surfaceContainerLow         = DarkSurfaceContainerLow,
    surfaceContainerLowest      = DarkSurfaceContainerLowest,
    outline              = DarkMuted,
    outlineVariant       = DarkOutlineVariant,
    error                = ErrorRed,
    onError              = OnError,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    scrim                = Color(0xFF000000),
    inverseSurface       = LightSurface,
    inverseOnSurface     = LightOnSurface,
    inversePrimary       = LightPrimary,
)
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppColors.kt
git commit -m "feat(theme): warm premium adaptive colour scheme"
```

---

## Task 4 — Rewrite AppTheme.kt (Adaptive Dark/Light)

**Files:**
- Rewrite: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTheme.kt`

- [ ] **Step 1: Rewrite AppTheme.kt**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) {
        WarmPremiumDarkColorScheme
    } else {
        WarmPremiumLightColorScheme
    }
    val typography = rememberAppTypography()
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        shapes      = AppShapes,
        content     = content,
    )
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :composeApp:compileKotlinAndroid --daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTheme.kt
git commit -m "feat(theme): adaptive dark/light mode via isSystemInDarkTheme"
```

---

## Task 5 — Update AppShapes.kt + Create AppMotion.kt

**Files:**
- Rewrite: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppShapes.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppMotion.kt`

- [ ] **Step 1: Rewrite AppShapes.kt**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Design spec §4 — Shape & Corner Radius
// shape_small   → 6dp   inputs, chips, badges
// shape_medium  → 14dp  list cards, modals
// shape_large   → 20dp  hero cards, bottom sheets
// shape_full    → ∞     buttons, pill nav, avatars
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(percent = 50), // pill / shape_full
    small      = RoundedCornerShape(6.dp),         // shape_small
    medium     = RoundedCornerShape(14.dp),        // shape_medium
    large      = RoundedCornerShape(20.dp),        // shape_large
    extraLarge = RoundedCornerShape(20.dp),        // also shape_large for sheets
)
```

- [ ] **Step 2: Create AppMotion.kt**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

// Design spec §8 — Motion / Hybrid Spring System
// Never use LinearEasing or EaseInOut. Always use spring().
object AppMotion {
    /** Card → Detail shared element expand */
    val cardToDetail = spring<Float>(dampingRatio = 0.75f, stiffness = 200f)

    /** Floating pill indicator slide between tabs */
    val pillIndicator = spring<Float>(dampingRatio = 0.85f, stiffness = 280f)

    /** Screen push (slide from right / bottom sheet slide up) */
    val screenPush = spring<Float>(dampingRatio = 0.80f, stiffness = 250f)

    /** Button / card press scale 1.0 → 0.97 → 1.0 */
    val press = spring<Float>(dampingRatio = 0.60f, stiffness = 400f)

    /** List item stagger fade + 8dp Y slide — apply 40ms delay per index */
    val listItem = spring<Float>(dampingRatio = 0.85f, stiffness = 260f)

    /** Press target scale — apply with animateFloatAsState + AppMotion.press */
    const val PRESS_SCALE = 0.97f

    /** Stagger delay per list item in milliseconds (max 5 items) */
    const val STAGGER_DELAY_MS = 40
}
```

- [ ] **Step 3: Verify build**

```bash
./gradlew :composeApp:compileKotlinAndroid --daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppShapes.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppMotion.kt
git commit -m "feat(theme): new shape tokens and spring motion constants"
```

---

## Task 6 — Build FloatingPillNav

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/FloatingPillNav.kt`
- The existing `BottomNavBar.kt` stays on disk but will be unused after Task 7; delete it then.

- [ ] **Step 1: Create FloatingPillNav.kt**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.hanmaum.dn.mobile.core.navigation.TopLevelDestination
import com.hanmaum.dn.mobile.core.presentation.theme.AppMotion
import kotlin.math.roundToInt

@Composable
fun FloatingPillNav(
    currentDestination: NavDestination?,
    onDestinationSelected: (TopLevelDestination<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val destinations = TopLevelDestination.all
    val selectedIndex = remember(currentDestination) {
        destinations.indexOfFirst { dest ->
            currentDestination?.hasRoute(dest.routeClass) == true
        }.coerceAtLeast(0)
    }

    val density = LocalDensity.current
    val bottomInsetPx = WindowInsets.navigationBars.getBottom(density)
    val bottomInsetDp = with(density) { bottomInsetPx.toDp() }

    var pillWidthPx by remember { mutableIntStateOf(0) }
    val itemWidthPx = if (destinations.isNotEmpty()) pillWidthPx / destinations.size else 0

    val indicatorOffsetPx by animateFloatAsState(
        targetValue = (selectedIndex * itemWidthPx).toFloat(),
        animationSpec = AppMotion.pillIndicator,
        label = "pillIndicator",
    )

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = bottomInsetDp + 16.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xDD2C1A0E))
            .onSizeChanged { pillWidthPx = it.width },
    ) {
        // Sliding active-tab indicator
        if (itemWidthPx > 0) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffsetPx.roundToInt(), 0) }
                    .width(with(density) { itemWidthPx.toDp() })
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0x30C4A882)),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            destinations.forEachIndexed { index, dest ->
                PillNavItem(
                    destination = dest,
                    selected = index == selectedIndex,
                    onClick = { onDestinationSelected(dest) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun PillNavItem(
    destination: TopLevelDestination<*>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                fadeIn(AppMotion.pillIndicator) togetherWith fadeOut(AppMotion.pillIndicator)
            },
            label = "pillItemContent_${destination.label}",
        ) { isSelected ->
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        tint = Color(0xFFC4A882),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFC4A882),
                    )
                }
            } else {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    tint = Color(0x66C4A882),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :composeApp:compileKotlinAndroid --daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/FloatingPillNav.kt
git commit -m "feat(nav): add FloatingPillNav with spring indicator"
```

---

## Task 7 — Wire FloatingPillNav into App.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/BottomNavBar.kt`

The Scaffold's `bottomBar` is removed. The NavHost is wrapped in a `Box` so the pill can overlay it. A `80.dp` bottom padding is applied to the NavHost so content doesn't hide behind the pill.

- [ ] **Step 1: Remove bottomBar from Scaffold in App.kt and add FloatingPillNav overlay**

Replace the entire `Scaffold { ... NavHost ... }` block with:

```kotlin
Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .statusBarsPadding(),
    ) {
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) 80.dp else 0.dp),
        ) {
            // ... all composable routes unchanged ...
        }

        if (showBottomBar) {
            FloatingPillNav(
                currentDestination = currentDestination,
                onDestinationSelected = { dest ->
                    navController.navigate(dest.routeInstance) {
                        popUpTo<HomeRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
```

Also add to the import section:
```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import com.hanmaum.dn.mobile.core.presentation.components.FloatingPillNav
```

And remove the import:
```kotlin
import com.hanmaum.dn.mobile.core.presentation.components.BottomNavBar
```

- [ ] **Step 2: Delete BottomNavBar.kt**

```bash
rm composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/BottomNavBar.kt
```

- [ ] **Step 3: Verify build**

```bash
./gradlew :composeApp:compileKotlinAndroid --daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run all tests**

```bash
./gradlew :composeApp:testDevDebugUnitTest
```

Expected: all tests pass (ViewModel tests are unaffected by navigation changes). Note: `allTests` is ambiguous in this project (dev/prod/st flavours) — use the flavour-specific task.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/BottomNavBar.kt
git commit -m "feat(nav): replace BottomNavBar with FloatingPillNav overlay"
```

---

## Task 8 — Rewrite AppTopBar.kt

**Files:**
- Rewrite: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/AppTopBar.kt`

Design spec §9: back icon is a chevron `<`, `muted` colour, 44dp touch target. No label. Transparent background.

- [ ] **Step 1: Rewrite AppTopBar.kt**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    onBackClick: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    TopAppBar(
        title = {},
        navigationIcon = {
            if (onBackClick != null) {
                // 44dp touch target wrapping a 24dp chevron icon
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = strings.back,
                            tint = MaterialTheme.colorScheme.outline, // muted colour
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            navigationIconContentColor = MaterialTheme.colorScheme.outline,
        ),
    )
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :composeApp:compileKotlinAndroid --daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/AppTopBar.kt
git commit -m "feat(nav): chevron back icon, transparent top bar, muted colour"
```

---

## Task 9 — Enable Android Predictive Back

**Files:**
- Modify: `composeApp/src/androidMain/AndroidManifest.xml`

Android 14+ (API 34+) predictive back requires the flag on the `<activity>` element. The `navigation-compose` library handles the gesture animation automatically once this is enabled.

- [ ] **Step 1: Add enableOnBackInvokedCallback to AndroidManifest.xml**

In `<activity android:name=".MainActivity">`, add the attribute:

```xml
<activity
    android:exported="true"
    android:name=".MainActivity"
    android:enableOnBackInvokedCallback="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :composeApp:assembleDebug --daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/androidMain/AndroidManifest.xml
git commit -m "feat(nav): enable Android predictive back gesture"
```

---

## Task 10 — Final Verification

- [ ] **Step 1: Run full test suite**

```bash
./gradlew :composeApp:testDevDebugUnitTest
```

Expected: all tests pass. Note: `allTests` is ambiguous in this project (dev/prod/st flavours) — use the flavour-specific task.

- [ ] **Step 2: Build debug APK**

```bash
./gradlew :composeApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. APK at `composeApp/build/outputs/apk/st/debug/`.

- [ ] **Step 3: Smoke-check on device/emulator**

Install the APK and verify:
- [ ] App launches — warm cream background, Pretendard font visible
- [ ] Dark mode toggle (system setting) — app switches to espresso dark
- [ ] Floating pill nav visible at bottom, icon-only for inactive tabs, icon+label for active
- [ ] Pill indicator slides with spring when switching tabs
- [ ] Tapping any detail screen shows chevron `<` back icon (not arrow)
- [ ] Android 14+ device: swipe from left edge shows predictive back preview

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: design system v2 complete — warm premium, Pretendard, floating pill"
```
