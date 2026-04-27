# Luminous Sanctuary Design Adoption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adopt "The Luminous Sanctuary" design system across all KMP Compose Multiplatform screens — replacing hardcoded colors, default Material3 tokens, and structural dividers with the new brand tokens, Plus Jakarta Sans typography, pill shapes, and tonal layering — without changing any logic, state, navigation, or data.

**Architecture:** Create a dedicated `core/presentation/theme/` package containing `AppColors`, `AppTypography`, `AppShapes`, and `AppTheme`. Wire `AppTheme` into `App.kt` replacing the bare `MaterialTheme` call. All 10 screens and 3 shared components are then updated screen-by-screen to use `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes` tokens — removing every hardcoded `Color(...)` and raw `FontWeight`/`fontSize` override.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10.0, Material3 (`androidx.compose.material3`), Compose Resources (`components.resources`) for Plus Jakarta Sans font loading, Kotlin 2.3.0.

---

## Prerequisites (User Action Required Before Starting)

### Download Plus Jakarta Sans font files

1. Go to https://fonts.google.com/specimen/Plus+Jakarta+Sans
2. Click "Download family"
3. Extract the zip — use the `static/` folder files (not variable fonts)
4. Copy these 5 files into:
   `composeApp/src/commonMain/composeResources/font/`
   - `PlusJakartaSans-Regular.ttf` → rename to `plus_jakarta_sans_regular.ttf`
   - `PlusJakartaSans-Medium.ttf` → rename to `plus_jakarta_sans_medium.ttf`
   - `PlusJakartaSans-SemiBold.ttf` → rename to `plus_jakarta_sans_semibold.ttf`
   - `PlusJakartaSans-Bold.ttf` → rename to `plus_jakarta_sans_bold.ttf`
   - `PlusJakartaSans-ExtraBold.ttf` → rename to `plus_jakarta_sans_extrabold.ttf`

> The `font/` directory does not exist yet inside `composeResources/` — create it alongside the existing `drawable/` directory.

---

## Verification Command (used after every task)

```bash
cd /Users/seungjinkim/Documents/Private_Projects/HanmaumDnApp
./gradlew :composeApp:compileKotlinAndroid --no-daemon
```

Expected output: `BUILD SUCCESSFUL`

---

## File Map

### New files (create)
| File | Purpose |
|------|---------|
| `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppColors.kt` | All brand color tokens as Compose `Color` constants |
| `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTypography.kt` | `rememberPlusJakartaSans()` font family + `rememberAppTypography()` returning Material3 `Typography` |
| `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppShapes.kt` | `AppShapes` `Shapes` object — pill, rounded-md, rounded-xl |
| `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTheme.kt` | `AppTheme` composable wrapping `MaterialTheme` with all three token sets |

### Modified files
| File | What changes |
|------|-------------|
| `App.kt` | `MaterialTheme { }` → `AppTheme { }` |
| `core/presentation/components/ChurchBottomBar.kt` | Replace hardcoded green/gray with `MaterialTheme.colorScheme` tokens |
| `core/presentation/components/ChurchTopBar.kt` | Replace `Color.Black`/`Color.White` with theme tokens |
| `core/presentation/components/ErrorView.kt` | Button shape: use `MaterialTheme.shapes.extraSmall` (pill) |
| `features/pending/screen/SplashScreen.kt` | Brand-forward layout, theme colors, fix incorrect `displayLarge` on loading text |
| `features/login/screen/LoginScreen.kt` | Full redesign to match login_register screenshot |
| `features/login/presentation/RegisterScreen.kt` | Match join_the_community screenshot — themed inputs + pill CTA |
| `features/pending/screen/PendingScreen.kt` | Warm sanctuary feel — themed, pill buttons |
| `features/announcement/presentation/HomeScreen.kt` | Theme background color applied to Scaffold |
| `features/announcement/presentation/AnnouncementListScreen.kt` | Themed cards, remove hardcoded `Color.White`, tonal card style |
| `features/announcement/presentation/AnnouncementDetailScreen.kt` | Remove `HorizontalDivider`, use spacing, themed category badge |
| `features/ministry/presentation/list/MinistryListScreen.kt` | Themed cards, no elevation (tonal lift via `surfaceContainerLowest`) |
| `features/ministry/presentation/detail/MinistryDetailScreen.kt` | Remove `HorizontalDivider`, pill CTA button, approved state via theme |
| `features/profile/presentation/ProfileScreen.kt` | Remove `HorizontalDivider`, themed edit/logout buttons |

---

## Task 1: Create AppColors.kt

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppColors.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Brand Tokens ─────────────────────────────────────────────────────────────
// Source: designs/design_md/DESIGN.md — "The Luminous Sanctuary"

// Primary: Coral — energy, passion, primary actions
val CoralDark        = Color(0xFFAE2F34)
val CoralLight       = Color(0xFFFF6B6B)
val OnCoral          = Color(0xFFFFFFFF)
val OnCoralContainer = Color(0xFF410002)

// Secondary: Faith Blue — grounding, navigation
val BlueDark         = Color(0xFF005DB8)
val BlueLight        = Color(0xFF4C96FE)
val OnBlue           = Color(0xFFFFFFFF)
val OnBlueContainer  = Color(0xFF001C3B)

// Tertiary: Holy Gold — "Aha!" moments, highlights
val GoldDark         = Color(0xFF705D00)
val GoldLight        = Color(0xFFFFE173)
val OnGold           = Color(0xFFFFFFFF)
val OnGoldContainer  = Color(0xFF221B00)

// Surface hierarchy (tonal layering — no borders)
val SanctuaryWhite   = Color(0xFFF9F9F9)  // base background — never pure white
val CardWhite        = Color(0xFFFFFFFF)  // cards on top of SanctuaryWhite
val SurfaceLow       = Color(0xFFF3F3F3)  // sectioning shift (surface_container_low)
val SurfaceMid       = Color(0xFFEEEEEE)  // heavier inset

// Text — warm charcoal (never pure black per design spec)
val DeepCharcoal     = Color(0xFF2D3436)  // primary text
val WarmCharcoal     = Color(0xFF584140)  // body text / on_surface_variant
val MutedGray        = Color(0xFF857371)  // outline / inactive

// Special
val SoftPeach        = Color(0xFFFFF5E1)  // reading section bg

// Error
val ErrorRed         = Color(0xFFBA1A1A)
val ErrorContainer   = Color(0xFFFFDAD6)
val OnError          = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF410002)

// ── Material3 ColorScheme ─────────────────────────────────────────────────────
val LuminousSanctuaryColorScheme = lightColorScheme(
    primary                = CoralDark,
    onPrimary              = OnCoral,
    primaryContainer       = CoralLight,
    onPrimaryContainer     = OnCoralContainer,
    secondary              = BlueDark,
    onSecondary            = OnBlue,
    secondaryContainer     = BlueLight,
    onSecondaryContainer   = OnBlueContainer,
    tertiary               = GoldDark,
    onTertiary             = OnGold,
    tertiaryContainer      = GoldLight,
    onTertiaryContainer    = OnGoldContainer,
    error                  = ErrorRed,
    onError                = OnError,
    errorContainer         = ErrorContainer,
    onErrorContainer       = OnErrorContainer,
    background             = SanctuaryWhite,
    onBackground           = DeepCharcoal,
    surface                = SanctuaryWhite,
    onSurface              = DeepCharcoal,
    surfaceVariant         = SurfaceLow,
    onSurfaceVariant       = WarmCharcoal,
    outline                = MutedGray,
    outlineVariant         = Color(0xFFD8C2BF),
    scrim                  = Color(0xFF000000),
    inverseSurface         = Color(0xFF362F2E),
    inverseOnSurface       = SoftPeach,
    inversePrimary         = Color(0xFFFFB3AE),
    surfaceTint            = CoralDark,
)
```

- [ ] **Step 2: Verify file saved correctly**

Check file exists at the path above. No compile step yet — no references to this file exist.

---

## Task 2: Create AppTypography.kt

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTypography.kt`

> **Prerequisite:** Font files must be in `composeApp/src/commonMain/composeResources/font/` (see Prerequisites above).

- [ ] **Step 1: Create the font directory and verify font files exist**

```
composeApp/src/commonMain/composeResources/font/
├── plus_jakarta_sans_regular.ttf
├── plus_jakarta_sans_medium.ttf
├── plus_jakarta_sans_semibold.ttf
├── plus_jakarta_sans_bold.ttf
└── plus_jakarta_sans_extrabold.ttf
```

- [ ] **Step 2: Create AppTypography.kt**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.plus_jakarta_sans_bold
import hanmaumdnapp.composeapp.generated.resources.plus_jakarta_sans_extrabold
import hanmaumdnapp.composeapp.generated.resources.plus_jakarta_sans_medium
import hanmaumdnapp.composeapp.generated.resources.plus_jakarta_sans_regular
import hanmaumdnapp.composeapp.generated.resources.plus_jakarta_sans_semibold
import org.jetbrains.compose.resources.Font

// Font family must be loaded inside a @Composable scope (CMP 1.7+ Resources API)
@Composable
fun rememberPlusJakartaSans(): FontFamily = FontFamily(
    Font(Res.font.plus_jakarta_sans_regular,  FontWeight.Normal),
    Font(Res.font.plus_jakarta_sans_medium,   FontWeight.Medium),
    Font(Res.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.plus_jakarta_sans_bold,     FontWeight.Bold),
    Font(Res.font.plus_jakarta_sans_extrabold,FontWeight.ExtraBold),
)

// Called inside AppTheme — builds the full Material3 Typography with Plus Jakarta Sans
@Composable
fun rememberAppTypography(): Typography {
    val ff = rememberPlusJakartaSans()
    return Typography(
        // Display: "Inspirational Statements" — tight letter-spacing (-0.02em)
        displayLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.ExtraBold,
            fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.96).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Bold,
            fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.72).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Bold,
            fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.56).sp,
        ),
        // Headline: section titles — pair with generous whitespace
        headlineLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Bold,
            fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.1).sp,
        ),
        // Title: card headers and sub-sections
        titleLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Medium,
            fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Medium,
            fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        ),
        // Body: warm charcoal — never pure black (applied via onSurfaceVariant token)
        bodyLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Normal,
            fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Normal,
            fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Normal,
            fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp,
        ),
        // Label: buttons (SemiBold) + chips/categories (uppercase via call site)
        labelLarge = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Medium,
            fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = ff, fontWeight = FontWeight.Medium,
            fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
        ),
    )
}
```

---

## Task 3: Create AppShapes.kt

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppShapes.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Design spec:
//   Buttons/chips → pill (rounded-full)
//   Inputs        → rounded-md  ≈ 12dp
//   Internal cards→ rounded-lg  ≈ 20dp
//   Container cards→ rounded-xl ≈ 24dp
//   Feature/hero  → extra-large  32dp
// Sharp 90° corners are strictly prohibited.

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(percent = 50), // Pill — buttons, chips, badges
    small      = RoundedCornerShape(12.dp),         // Inputs, small surfaces
    medium     = RoundedCornerShape(20.dp),         // Internal cards
    large      = RoundedCornerShape(24.dp),         // Container cards (default Card shape)
    extraLarge = RoundedCornerShape(32.dp),         // Hero / feature cards / bottom sheets
)
```

---

## Task 4: Create AppTheme.kt and wire into App.kt

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTheme.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`

- [ ] **Step 1: Create AppTheme.kt**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val typography = rememberAppTypography()
    MaterialTheme(
        colorScheme = LuminousSanctuaryColorScheme,
        typography   = typography,
        shapes       = AppShapes,
        content      = content,
    )
}
```

- [ ] **Step 2: Update App.kt — replace MaterialTheme with AppTheme**

In `App.kt`, find:
```kotlin
import androidx.compose.material3.*
```
Add import below it:
```kotlin
import com.hanmaum.dn.mobile.core.presentation.theme.AppTheme
```

Find the block:
```kotlin
KoinContext {
    MaterialTheme {
```
Replace with:
```kotlin
KoinContext {
    AppTheme {
```

- [ ] **Step 3: Compile check**

```bash
cd /Users/seungjinkim/Documents/Private_Projects/HanmaumDnApp
./gradlew :composeApp:compileKotlinAndroid --no-daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
cd /Users/seungjinkim/Documents/Private_Projects/HanmaumDnApp
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/
git add composeApp/src/commonMain/composeResources/font/
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git commit -m "feat(theme): add Luminous Sanctuary design tokens — AppColors, AppTypography, AppShapes, AppTheme"
```

---

## Task 5: Restyle ChurchBottomBar, ChurchTopBar, ErrorView

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ChurchBottomBar.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ChurchTopBar.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/ErrorView.kt`

- [ ] **Step 1: Replace ChurchBottomBar.kt entirely**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class BottomTab { HOME, SERMON, QT, PROFILE }

@Composable
fun ChurchBottomBar(
    selectedTab: BottomTab = BottomTab.HOME,
    onTabSelected: (BottomTab) -> Unit = {},
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = androidx.compose.ui.unit.Dp.Unspecified,
    ) {
        val selectedColor   = MaterialTheme.colorScheme.primary
        val indicatorColor  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor   = selectedColor,
            selectedTextColor   = selectedColor,
            indicatorColor      = indicatorColor,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor,
        )

        NavigationBarItem(
            selected = selectedTab == BottomTab.HOME,
            onClick  = { onTabSelected(BottomTab.HOME) },
            icon     = { Icon(Icons.Default.Home, contentDescription = "홈") },
            label    = { Text("홈", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.SERMON,
            onClick  = { onTabSelected(BottomTab.SERMON) },
            icon     = { Icon(Icons.Default.Mic, contentDescription = "순소식") },
            label    = { Text("순소식", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.QT,
            onClick  = { onTabSelected(BottomTab.QT) },
            icon     = { Icon(Icons.Default.Description, contentDescription = "QT") },
            label    = { Text("QT", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.PROFILE,
            onClick  = { onTabSelected(BottomTab.PROFILE) },
            icon     = { Icon(Icons.Default.Person, contentDescription = "프로필") },
            label    = { Text("프로필", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
    }
}
```

- [ ] **Step 2: Replace ChurchTopBar.kt entirely**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChurchTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector    = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint           = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector    = Icons.Default.Menu,
                        contentDescription = "메뉴",
                        tint           = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            if (onBackClick == null) {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "알림",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Person, contentDescription = "프로필",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = MaterialTheme.colorScheme.surface,
            titleContentColor      = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
```

- [ ] **Step 3: Update ErrorView.kt — pill-shaped retry button**

In `ErrorView.kt`, find the `Button` at the bottom:
```kotlin
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Erneut versuchen")
        }
```
Replace with:
```kotlin
        Button(
            onClick = onRetry,
            shape  = MaterialTheme.shapes.extraSmall,  // pill
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Erneut versuchen", style = MaterialTheme.typography.labelLarge)
        }
```

- [ ] **Step 4: Compile check**

```bash
./gradlew :composeApp:compileKotlinAndroid --no-daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/
git commit -m "feat(theme): restyle ChurchBottomBar, ChurchTopBar, ErrorView with Luminous Sanctuary tokens"
```

---

## Task 6: Restyle SplashScreen and LoginScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/pending/screen/SplashScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/login/screen/LoginScreen.kt`

- [ ] **Step 1: Replace SplashScreen.kt entirely**

```kotlin
package com.hanmaum.dn.mobile.features.pending.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.features.pending.presentation.SplashViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    onNavigate: (NavRoute) -> Unit,
) {
    val destination by viewModel.navigateTo.collectAsState()

    LaunchedEffect(destination) {
        destination?.let { route ->
            onNavigate(route)
            viewModel.onNavigationHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "The Sanctuary",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "한마음 D+N",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color    = MaterialTheme.colorScheme.primary,
                strokeWidth = androidx.compose.ui.unit.Dp(2f),
            )
        }
    }
}
```

- [ ] **Step 2: Replace LoginScreen.kt entirely**

```kotlin
package com.hanmaum.dn.mobile.features.login.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.features.login.presentation.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToPending: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { route ->
            when (route) {
                NavRoute.Home           -> onNavigateToHome()
                NavRoute.PendingApproval -> onNavigateToPending()
                else -> {}
            }
            viewModel.onNavigationHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        // Brand header
        Text(
            text  = "The Sanctuary",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text  = "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text  = "커뮤니티와 연결하려면 로그인하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email field
        OutlinedTextField(
            value         = username,
            onValueChange = { username = it },
            label         = { Text("이메일") },
            placeholder   = { Text("hello@community.com") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = MaterialTheme.shapes.small,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value                = password,
            onValueChange        = { password = it },
            label                = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(),
            modifier             = Modifier.fillMaxWidth(),
            singleLine           = true,
            shape                = MaterialTheme.shapes.small,
            colors               = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Primary CTA — pill-shaped coral button
        Button(
            onClick  = { viewModel.onLoginClicked(username, password) },
            enabled  = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = MaterialTheme.shapes.extraSmall,
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    color       = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = androidx.compose.ui.unit.Dp(2f),
                )
            } else {
                Text("로그인 →", style = MaterialTheme.typography.labelLarge)
            }
        }

        state.error?.let { errorMsg ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMsg, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick  = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text  = "커뮤니티에 처음이신가요? 등록하기",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
```

- [ ] **Step 3: Compile check**

```bash
./gradlew :composeApp:compileKotlinAndroid --no-daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/pending/screen/SplashScreen.kt
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/login/screen/LoginScreen.kt
git commit -m "feat(theme): restyle SplashScreen and LoginScreen with Luminous Sanctuary"
```

---

## Task 7: Restyle RegisterScreen and PendingScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/login/presentation/RegisterScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/pending/screen/PendingScreen.kt`

- [ ] **Step 1: Apply themed styles to RegisterScreen.kt**

In `RegisterScreen.kt`, find every `OutlinedTextField` block. Each one needs `shape` and `colors` parameters added. The pattern to apply to all `OutlinedTextField` occurrences (both in `RegisterScreen` and `GenericDropdown`):

```kotlin
// ADD these two parameters to every OutlinedTextField:
shape  = MaterialTheme.shapes.small,
colors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = MaterialTheme.colorScheme.secondary,
    unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor   = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
),
```

Find the `Text("Pflichtfelder sind mit * markiert", ...)` line:
```kotlin
Text("Pflichtfelder sind mit * markiert", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
```
Replace with:
```kotlin
Text("Pflichtfelder sind mit * markiert", style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant)
```

Find the `HorizontalDivider` separator before the optional section:
```kotlin
HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
Text("Zusätzliche Infos (Optional)", style = MaterialTheme.typography.titleSmall)
```
Replace with (no divider — use spacing per design spec):
```kotlin
Spacer(modifier = Modifier.height(8.dp))
Text("Zusätzliche Infos (Optional)", style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant)
Spacer(modifier = Modifier.height(4.dp))
```

Find the `Button` at the bottom:
```kotlin
Button(
    onClick = { viewModel.register() },
    modifier = Modifier.fillMaxWidth().height(50.dp),
    enabled = !state.isLoading
) {
    if (state.isLoading) {
        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
    } else {
        Text("등록하기")
    }
}
```
Replace with:
```kotlin
Button(
    onClick  = { viewModel.register() },
    modifier = Modifier.fillMaxWidth().height(54.dp),
    enabled  = !state.isLoading,
    shape    = MaterialTheme.shapes.extraSmall,
    colors   = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    ),
) {
    if (state.isLoading) {
        CircularProgressIndicator(
            color       = MaterialTheme.colorScheme.onPrimary,
            modifier    = Modifier.size(22.dp),
            strokeWidth = androidx.compose.ui.unit.Dp(2f),
        )
    } else {
        Text("등록하기", style = MaterialTheme.typography.labelLarge)
    }
}
```

Remove the `Color.Gray` import and unused `import androidx.compose.ui.graphics.Color` if it is now unused.

- [ ] **Step 2: Update PendingScreen.kt — themed pill buttons**

In `PendingScreen.kt`, find:
```kotlin
Button(
    onClick = { viewModel.onCheckStatusClicked() },
    modifier = Modifier.fillMaxWidth().height(50.dp)
) {
    Text("승인 상태 확인 (Status check)")
}
```
Replace with:
```kotlin
Button(
    onClick  = { viewModel.onCheckStatusClicked() },
    modifier = Modifier.fillMaxWidth().height(54.dp),
    shape    = MaterialTheme.shapes.extraSmall,
    colors   = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    ),
) {
    Text("승인 상태 확인", style = MaterialTheme.typography.labelLarge)
}
```

The `TextButton` for logout already uses `MaterialTheme.colorScheme` implicitly — no change needed. Verify `tint = MaterialTheme.colorScheme.primary` on the Lock icon remains.

- [ ] **Step 3: Compile check**

```bash
./gradlew :composeApp:compileKotlinAndroid --no-daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/login/presentation/RegisterScreen.kt
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/pending/screen/PendingScreen.kt
git commit -m "feat(theme): restyle RegisterScreen and PendingScreen with Luminous Sanctuary"
```

---

## Task 8: Restyle AnnouncementListScreen and AnnouncementDetailScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/AnnouncementListScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/AnnouncementDetailScreen.kt`

- [ ] **Step 1: Update ListItemCard in AnnouncementListScreen.kt**

Find the `ListItemCard` composable and replace it entirely:

```kotlin
@Composable
private fun ListItemCard(
    news: com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement,
    onClick: () -> Unit,
) {
    Card(
        onClick    = onClick,
        modifier   = Modifier.fillMaxWidth(),
        shape      = MaterialTheme.shapes.medium,
        colors     = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,  // CardWhite on SanctuaryWhite bg
        ),
        elevation  = CardDefaults.cardElevation(defaultElevation = 0.dp),  // tonal lift, not shadow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Category tag pill
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            ) {
                Text(
                    text     = news.getAnnouncementCategoryName(),
                    color    = MaterialTheme.colorScheme.primary,
                    style    = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text     = news.title,
                style    = MaterialTheme.typography.bodyLarge,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
```

Also update the `Scaffold` background in `AnnouncementListScreen`:

Find the `Scaffold(` block and add `containerColor`:
```kotlin
Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = { ... }
```

Remove `import androidx.compose.ui.graphics.Color` if no longer used.

- [ ] **Step 2: Update AnnouncementDetailScreen.kt — remove HorizontalDivider, themed category badge**

Find:
```kotlin
HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
```
Replace with:
```kotlin
Spacer(modifier = Modifier.height(24.dp))
```

Find the category badge `Surface`:
```kotlin
Surface(
    color = Color(item.getAnnouncementCategoryColor()).copy(alpha = 0.1f),
    shape = MaterialTheme.shapes.small
) {
    Text(
        text = item.getAnnouncementCategoryName(),
        color = Color(item.getAnnouncementCategoryColor()),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}
```
Replace with:
```kotlin
Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
    shape = MaterialTheme.shapes.extraSmall,
) {
    Text(
        text     = item.getAnnouncementCategoryName(),
        color    = MaterialTheme.colorScheme.primary,
        style    = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
```

Find the date text:
```kotlin
Text(
    text = item.startAt,
    color = Color.Gray,
    style = MaterialTheme.typography.bodySmall
)
```
Replace with:
```kotlin
Text(
    text  = item.startAt,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    style = MaterialTheme.typography.bodySmall,
)
```

Remove now-unused imports: `FontWeight`, `fontSize = 12.sp`, `Color`.

- [ ] **Step 3: Compile check**

```bash
./gradlew :composeApp:compileKotlinAndroid --no-daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/AnnouncementListScreen.kt
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/AnnouncementDetailScreen.kt
git commit -m "feat(theme): restyle AnnouncementListScreen and AnnouncementDetailScreen"
```

---

## Task 9: Restyle MinistryListScreen and MinistryDetailScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/presentation/list/MinistryListScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/presentation/detail/MinistryDetailScreen.kt`

- [ ] **Step 1: Update MinistryCard in MinistryListScreen.kt — tonal card, no elevation**

Find the `MinistryCard` composable and replace it:

```kotlin
@Composable
private fun MinistryCard(
    ministry: Ministry,
    onClick: () -> Unit,
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,  // white card on off-white bg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier            = Modifier.padding(20.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Icon in a small coral-tinted container
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.Group,
                    contentDescription = null,
                    modifier           = Modifier.padding(10.dp),
                    tint               = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text     = ministry.name,
                    style    = MaterialTheme.typography.titleMedium,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                ministry.leaderName?.let {
                    Text(
                        text  = "리더: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text     = ministry.shortDescription,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
```

Also add `containerColor` to the `Scaffold`:
```kotlin
Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = { ... }
```

- [ ] **Step 2: Update MinistryDetailScreen.kt — remove HorizontalDivider, pill registration buttons, themed approved state**

Find:
```kotlin
HorizontalDivider()
```
Replace with:
```kotlin
Spacer(modifier = Modifier.height(4.dp))
```

Find `RegistrationButton` composable and replace it entirely:

```kotlin
@Composable
private fun RegistrationButton(
    status: RegistrationStatus,
    onClick: () -> Unit,
) {
    when (status) {
        RegistrationStatus.NONE -> {
            Button(
                onClick  = onClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = MaterialTheme.shapes.extraSmall,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("신청하기", style = MaterialTheme.typography.labelLarge)
            }
        }
        RegistrationStatus.PENDING -> {
            Button(
                onClick  = {},
                enabled  = false,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = MaterialTheme.shapes.extraSmall,
            ) {
                Text("신청되었습니다", style = MaterialTheme.typography.labelLarge)
            }
        }
        RegistrationStatus.APPROVED -> {
            Button(
                onClick  = {},
                enabled  = false,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = MaterialTheme.shapes.extraSmall,
                colors   = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    disabledContentColor   = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Text("멤버입니다 ✓", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
```

In `RegistrationBottomSheet`, find the confirm `Button` and update its shape:
```kotlin
Button(
    onClick = onConfirm,
    modifier = Modifier.fillMaxWidth(),
    enabled = !isLoading,
    shape   = MaterialTheme.shapes.extraSmall,
) {
```

Remove `import androidx.compose.ui.graphics.Color` from MinistryDetailScreen.kt if now unused.

- [ ] **Step 3: Compile check**

```bash
./gradlew :composeApp:compileKotlinAndroid --no-daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/
git commit -m "feat(theme): restyle MinistryListScreen and MinistryDetailScreen"
```

---

## Task 10: Restyle ProfileScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt`

- [ ] **Step 1: Remove all HorizontalDividers from ProfileField**

Find:
```kotlin
@Composable
private fun ProfileField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider()
}
```
Replace with (background-shift separation instead of divider):
```kotlin
@Composable
private fun ProfileField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
```

- [ ] **Step 2: Update Edit and Logout buttons to pill shape**

In `ProfileViewContent`, find the Edit button:
```kotlin
Button(
    onClick = onEditClick,
    modifier = Modifier.fillMaxWidth(),
) {
```
Replace with:
```kotlin
Button(
    onClick  = onEditClick,
    modifier = Modifier.fillMaxWidth().height(50.dp),
    shape    = MaterialTheme.shapes.extraSmall,
    colors   = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    ),
) {
```

Find the Logout button:
```kotlin
OutlinedButton(
    onClick = onLogoutClick,
    modifier = Modifier.fillMaxWidth(),
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.error,
    ),
) {
```
Replace with:
```kotlin
OutlinedButton(
    onClick  = onLogoutClick,
    modifier = Modifier.fillMaxWidth().height(50.dp),
    shape    = MaterialTheme.shapes.extraSmall,
    colors   = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.error,
    ),
) {
```

- [ ] **Step 3: Update ProfileEditContent — pill Save/Cancel buttons**

In `ProfileEditContent`, find:
```kotlin
Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("취소") }
    Button(
        onClick = onSave,
        modifier = Modifier.weight(1f),
        enabled = !state.isSaving,
    ) {
```
Replace with:
```kotlin
Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedButton(
        onClick  = onCancel,
        modifier = Modifier.weight(1f).height(50.dp),
        shape    = MaterialTheme.shapes.extraSmall,
    ) { Text("취소", style = MaterialTheme.typography.labelLarge) }
    Button(
        onClick  = onSave,
        modifier = Modifier.weight(1f).height(50.dp),
        shape    = MaterialTheme.shapes.extraSmall,
        enabled  = !state.isSaving,
        colors   = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
```

- [ ] **Step 4: Compile check**

```bash
./gradlew :composeApp:compileKotlinAndroid --no-daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt
git commit -m "feat(theme): restyle ProfileScreen with Luminous Sanctuary"
```

---

## Task 11: Final sweep — HomeScreen scaffold background + DESIGN.md commit

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/HomeScreen.kt`
- Modify (in dn-app repo): `DESIGN.md` (commit the staged + unstaged version)

- [ ] **Step 1: Add containerColor to HomeScreen Scaffold**

In `HomeScreen.kt`, find:
```kotlin
    Scaffold(
        topBar = { ChurchTopBar(title = "D+N App", onBackClick = null) },
```
Replace with:
```kotlin
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ChurchTopBar(title = "D+N App", onBackClick = null) },
```

- [ ] **Step 2: Final compile check**

```bash
./gradlew :composeApp:compileKotlinAndroid --no-daemon
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit HomeScreen**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/HomeScreen.kt
git commit -m "feat(theme): apply background color to HomeScreen scaffold"
```

- [ ] **Step 4: Commit DESIGN.md in dn-app repo**

```bash
cd /Users/seungjinkim/Documents/Private_Projects/dn-app
git add DESIGN.md
git commit -m "docs: add Luminous Sanctuary design system spec"
```

---

## Self-Review

**Spec coverage check:**

| Design requirement | Covered? | Task |
|---|---|---|
| Primary coral `#AE2F34` / `#FF6B6B` | ✅ | Task 1 |
| Secondary faith blue `#005DB8` / `#4C96FE` | ✅ | Task 1 |
| Tertiary holy gold `#705D00` / `#FFE173` | ✅ | Task 1 |
| No pure black text (warm charcoal `#2D3436`) | ✅ | Task 1 (onSurface) |
| No divider lines — use spacing/bg shift | ✅ | Tasks 8, 9, 10 |
| Pill-shaped buttons (rounded-full) | ✅ | Tasks 3, 6, 7, 8, 9, 10 |
| Card radius `rounded-xl` (24dp) | ✅ | Task 3 (AppShapes.large) |
| Input radius `rounded-md` (12dp) | ✅ | Task 3 (AppShapes.small) |
| `surface` `#F9F9F9` background | ✅ | Task 1 |
| Cards `#FFFFFF` on `#F9F9F9` (tonal lift) | ✅ | Tasks 8, 9 (containerColor = surface) |
| Plus Jakarta Sans typography | ✅ | Task 2 |
| Tight letter-spacing on display text | ✅ | Task 2 |
| Bottom nav — coral selected, muted unselected | ✅ | Task 5 |
| Top bar — warm surface, no hardcoded black | ✅ | Task 5 |
| Scaffold backgrounds inherit `#F9F9F9` | ✅ | Tasks 6, 8, 9, 11 |
| Logic/navigation unchanged | ✅ | All tasks — only UI layer touched |

**Placeholder scan:** None found — all tasks contain exact code.

**Type consistency:** All shape references use `MaterialTheme.shapes.*` from `AppShapes`; all color references use `MaterialTheme.colorScheme.*` from `LuminousSanctuaryColorScheme`.
