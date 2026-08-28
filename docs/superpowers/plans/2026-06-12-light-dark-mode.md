# Light / Dark Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a user-selectable, persisted theme (System / Light / Dark), make the in-app logo render black in light mode and white in dark mode, and ensure every screen reads `MaterialTheme.colorScheme.*` tokens so all surfaces and visuals flip correctly.

**Architecture:** Mirror the existing `LocaleRepository` pattern exactly — a settings-backed `ThemeRepository` whose value is hoisted as state in `App()`, fed into `AppTheme(themeMode)`, and changed from a picker in `ProfileScreen` (alongside the existing language picker). Both Warm-Premium color schemes already exist in `AppColors.kt`; no palette changes. The logo is a single transparent-background black PNG, tinted with `colorScheme.onSurface`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10.0 / Material3, Koin 4.x DI, `com.russhwolf:multiplatform-settings` (+ `multiplatform-settings-test` `MapSettings` for tests), `kotlin.test`.

**Spec:** `docs/superpowers/specs/2026-06-12-light-dark-mode-design.md`

---

## File Structure

**Create:**
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/model/ThemeMode.kt` — the `ThemeMode` enum (SYSTEM/LIGHT/DARK).
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/repository/ThemeRepository.kt` — interface.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/ThemeRepositoryImpl.kt` — settings-backed impl.
- `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/data/repository/ThemeRepositoryImplTest.kt` — unit test.

**Modify:**
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt` — bind `ThemeRepository`.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTheme.kt` — `themeMode` param + resolution.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt` — inject repo, hoist `themeMode`, pass to `AppTheme` + `ProfileScreen`.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStrings.kt` — 5 theme strings × interface + En/Ko/De.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt` — theme card + `ThemePickerSheet`, new params.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/pending/screen/SplashScreen.kt` — logo tint.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/login/screen/LoginScreen.kt` — logo tint.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/HeroBannerSection.kt` — token audit.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/LatestNewsSection.kt` — token audit.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/AnnouncementDetailScreen.kt` — token audit.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarScreen.kt` — token audit.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/presentation/detail/MinistryDetailScreen.kt` — token audit.

**Leave alone (intentional):** `core/presentation/components/FloatingPillNav` and the `Pill*` constants in `AppColors.kt` — dark-frosted in both modes by DESIGN.md §6. `RegisterScreen.kt` line 895 `successColor = Color(0xFF22A06B)` is a semantic status color, legible in both modes — out of scope.

**Build/test command note (from `tasks/lessons.md`):** product flavors make bare task names ambiguous. Use `:composeApp:testDevDebugUnitTest` for the JVM/Android unit tests and `:composeApp:assembleDevDebug` to compile. `:composeApp:allTests` additionally runs the iOS native test link which needs full Xcode; the common tests still run on the JVM/Android target.

---

## Task 1: ThemeMode enum

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/model/ThemeMode.kt`

- [ ] **Step 1: Create the enum**

```kotlin
package com.hanmaum.dn.mobile.core.domain.model

/** User-selectable app theme. SYSTEM follows the OS light/dark setting. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/model/ThemeMode.kt
git commit -m "feat(theme): add ThemeMode enum"
```

---

## Task 2: ThemeRepository + Impl (TDD)

Mirrors `LocaleRepository` / `LocaleRepositoryImpl` / `LocaleRepositoryImplTest` exactly.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/repository/ThemeRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/ThemeRepositoryImpl.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/data/repository/ThemeRepositoryImplTest.kt`

- [ ] **Step 1: Write the interface**

```kotlin
package com.hanmaum.dn.mobile.core.domain.repository

import com.hanmaum.dn.mobile.core.domain.model.ThemeMode

interface ThemeRepository {
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeRepositoryImplTest {

    private fun repo(settings: MapSettings = MapSettings()) = ThemeRepositoryImpl(settings)

    @Test
    fun `getThemeMode returns SYSTEM when no value stored`() {
        assertEquals(ThemeMode.SYSTEM, repo().getThemeMode())
    }

    @Test
    fun `setThemeMode persists and getThemeMode returns the same value`() {
        val settings = MapSettings()
        repo(settings).setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo(settings).getThemeMode())
    }

    @Test
    fun `setThemeMode LIGHT round-trips correctly`() {
        val settings = MapSettings()
        repo(settings).setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repo(settings).getThemeMode())
    }

    @Test
    fun `getThemeMode returns SYSTEM for unknown stored value`() {
        val settings = MapSettings("theme_mode" to "PURPLE")
        assertEquals(ThemeMode.SYSTEM, repo(settings).getThemeMode())
    }
}
```

- [ ] **Step 3: Run test to verify it fails (compile error — Impl does not exist)**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.core.data.repository.ThemeRepositoryImplTest"`
Expected: FAIL — unresolved reference `ThemeRepositoryImpl`.

- [ ] **Step 4: Write the implementation**

```kotlin
package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.domain.repository.ThemeRepository
import com.russhwolf.settings.Settings

class ThemeRepositoryImpl(private val settings: Settings) : ThemeRepository {

    override fun getThemeMode(): ThemeMode =
        settings.getStringOrNull(KEY)
            ?.let { stored -> ThemeMode.entries.find { it.name == stored } }
            ?: ThemeMode.SYSTEM

    override fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY, mode.name)
    }

    companion object {
        private const val KEY = "theme_mode"
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.core.data.repository.ThemeRepositoryImplTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/repository/ThemeRepository.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/ThemeRepositoryImpl.kt \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/data/repository/ThemeRepositoryImplTest.kt
git commit -m "feat(theme): add settings-backed ThemeRepository"
```

---

## Task 3: Bind ThemeRepository in Koin

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt`

- [ ] **Step 1: Add imports** (next to the existing Locale imports near the top of the file)

```kotlin
import com.hanmaum.dn.mobile.core.data.repository.ThemeRepositoryImpl
import com.hanmaum.dn.mobile.core.domain.repository.ThemeRepository
```

- [ ] **Step 2: Add the binding** directly after the existing `LocaleRepository` line

Find:
```kotlin
    single<LocaleRepository> { LocaleRepositoryImpl(Settings()) }
```
Add immediately below:
```kotlin
    single<ThemeRepository> { ThemeRepositoryImpl(Settings()) }
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt
git commit -m "feat(theme): bind ThemeRepository in Koin module"
```

---

## Task 4: AppTheme resolves ThemeMode

Give the param a default so the existing `App.kt` call site keeps compiling until Task 6 wires it.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTheme.kt`

- [ ] **Step 1: Replace the file contents**

```kotlin
package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    val colorScheme = if (useDark) WarmPremiumDarkColorScheme else WarmPremiumLightColorScheme
    val typography = rememberAppTypography()
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        shapes      = AppShapes,
        content     = content,
    )
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL (existing `AppTheme { ... }` call still valid via default param).

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/AppTheme.kt
git commit -m "feat(theme): resolve light/dark/system in AppTheme"
```

---

## Task 5: Add theme i18n strings

The `AppStrings` interface forces all three string objects to implement the new keys (compile error otherwise), so locale parity is compiler-guaranteed.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStrings.kt`

- [ ] **Step 1: Add to the interface** — directly after `val selectLanguage: String`

```kotlin
    val profileTheme: String
    val selectTheme: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
```

- [ ] **Step 2: Add to `EnStrings`** — directly after `override val selectLanguage = "Select Language"`

```kotlin
    override val profileTheme = "THEME"
    override val selectTheme = "Select Theme"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
```

- [ ] **Step 3: Add to `KoStrings`** — directly after `override val selectLanguage = "언어 선택"`

```kotlin
    override val profileTheme = "테마"
    override val selectTheme = "테마 선택"
    override val themeSystem = "시스템"
    override val themeLight = "라이트"
    override val themeDark = "다크"
```

- [ ] **Step 4: Add to `DeStrings`** — directly after `override val selectLanguage = "Sprache auswählen"`

```kotlin
    override val profileTheme = "DARSTELLUNG"
    override val selectTheme = "Darstellung wählen"
    override val themeSystem = "System"
    override val themeLight = "Hell"
    override val themeDark = "Dunkel"
```

- [ ] **Step 5: Run the strings tests + compile**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.core.i18n.AppStringsTest"`
Expected: PASS (all three objects implement the interface; no missing-override compile error).

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStrings.kt
git commit -m "feat(theme): add theme picker i18n strings (en/ko/de)"
```

---

## Task 6: Theme picker in Profile + App.kt wiring

ProfileScreen gains the params **and** App.kt provides them in the same commit so the build stays green. Mirrors the existing `currentLocale`/`onLocaleChange` + `LanguagePickerSheet` exactly.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`

- [ ] **Step 1: ProfileScreen — add import** (next to the existing `import com.hanmaum.dn.mobile.core.i18n.AppLocale`)

```kotlin
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
```

- [ ] **Step 2: ProfileScreen — extend the public composable signature**

Find:
```kotlin
fun ProfileScreen(
    onLogout: () -> Unit,
    currentLocale: AppLocale,
    onLocaleChange: (AppLocale) -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
```
Replace with:
```kotlin
fun ProfileScreen(
    onLogout: () -> Unit,
    currentLocale: AppLocale,
    onLocaleChange: (AppLocale) -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
```

- [ ] **Step 3: ProfileScreen — pass into `ProfileViewContent`**

Find:
```kotlin
                        ProfileViewContent(
                            profile        = state.profile,
                            currentLocale  = currentLocale,
                            onEditClick    = { viewModel.startEditing() },
                            onLogoutClick  = { viewModel.logout() },
                            onLocaleChange = onLocaleChange,
                        )
```
Replace with:
```kotlin
                        ProfileViewContent(
                            profile        = state.profile,
                            currentLocale  = currentLocale,
                            currentTheme   = currentTheme,
                            onEditClick    = { viewModel.startEditing() },
                            onLogoutClick  = { viewModel.logout() },
                            onLocaleChange = onLocaleChange,
                            onThemeChange  = onThemeChange,
                        )
```

- [ ] **Step 4: ProfileScreen — extend `ProfileViewContent` signature + local state**

Find:
```kotlin
private fun ProfileViewContent(
    profile: MemberResponse,
    currentLocale: AppLocale,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLocaleChange: (AppLocale) -> Unit,
) {
    val strings = LocalStrings.current
    var showLanguagePicker by remember { mutableStateOf(false) }
```
Replace with:
```kotlin
private fun ProfileViewContent(
    profile: MemberResponse,
    currentLocale: AppLocale,
    currentTheme: ThemeMode,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLocaleChange: (AppLocale) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val strings = LocalStrings.current
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
```

- [ ] **Step 5: ProfileScreen — add a Theme card** directly after the closing `}` of the language `Card { ... }` block (i.e. after the language card's closing brace, before `Spacer(Modifier.height(28.dp))`)

```kotlin
        Spacer(Modifier.height(12.dp))
        Card(
            modifier  = Modifier.fillMaxWidth(),
            onClick   = { showThemePicker = true },
            shape     = MaterialTheme.shapes.large,
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text  = strings.profileTheme,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = themeModeLabel(currentTheme, strings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
```

- [ ] **Step 6: ProfileScreen — render the picker** directly after the existing `if (showLanguagePicker) { LanguagePickerSheet(...) }` block

```kotlin
    if (showThemePicker) {
        ThemePickerSheet(
            currentTheme = currentTheme,
            onSelect     = { mode ->
                onThemeChange(mode)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false },
        )
    }
```

- [ ] **Step 7: ProfileScreen — add the label helper + the picker sheet** at the end of the file (after `LanguagePickerSheet`)

```kotlin
private fun themeModeLabel(mode: ThemeMode, strings: AppStrings): String = when (mode) {
    ThemeMode.SYSTEM -> strings.themeSystem
    ThemeMode.LIGHT  -> strings.themeLight
    ThemeMode.DARK   -> strings.themeDark
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerSheet(
    currentTheme: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                text  = strings.selectTheme,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(mode) }
                        .padding(vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = themeModeLabel(mode, strings),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (mode == currentTheme)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                    if (mode == currentTheme) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}
```

> Note: `themeModeLabel` references the `AppStrings` type — add `import com.hanmaum.dn.mobile.core.i18n.AppStrings` to ProfileScreen if not already present.

- [ ] **Step 8: App.kt — add imports** (next to the existing locale/theme imports)

```kotlin
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.domain.repository.ThemeRepository
```

- [ ] **Step 9: App.kt — inject repo + hoist state** directly after the existing locale hoist

Find:
```kotlin
        val localeRepo = koinInject<LocaleRepository>()
        var locale by remember { mutableStateOf(localeRepo.getLocale()) }
```
Add immediately below:
```kotlin
        val themeRepo = koinInject<ThemeRepository>()
        var themeMode by remember { mutableStateOf(themeRepo.getThemeMode()) }
```

- [ ] **Step 10: App.kt — pass themeMode into AppTheme**

Find:
```kotlin
            AppTheme {
```
Replace with:
```kotlin
            AppTheme(themeMode = themeMode) {
```

- [ ] **Step 11: App.kt — pass theme params into ProfileScreen**

Find:
```kotlin
                        ProfileScreen(
                            onLogout = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            currentLocale = locale,
                            onLocaleChange = { newLocale ->
                                localeRepo.setLocale(newLocale)
                                locale = newLocale
                            },
                        )
```
Replace with:
```kotlin
                        ProfileScreen(
                            onLogout = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            currentLocale = locale,
                            onLocaleChange = { newLocale ->
                                localeRepo.setLocale(newLocale)
                                locale = newLocale
                            },
                            currentTheme = themeMode,
                            onThemeChange = { newMode ->
                                themeRepo.setThemeMode(newMode)
                                themeMode = newMode
                            },
                        )
```

- [ ] **Step 12: Verify it compiles**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 13: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git commit -m "feat(theme): add theme picker in Profile and wire through App"
```

---

## Task 7: Tint the logo per mode

`onSurface` = espresso `#2C1A0E` in light (reads black) and cream `#F5E6CC` in dark (reads white). Background stays transparent.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/pending/screen/SplashScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/login/screen/LoginScreen.kt`

- [ ] **Step 1: SplashScreen — add import**

```kotlin
import androidx.compose.ui.graphics.ColorFilter
```

- [ ] **Step 2: SplashScreen — tint the logo**

Find:
```kotlin
        Image(
            painter            = painterResource(Res.drawable.logo),
            contentDescription = "Daniel & Nehemia logo",
            modifier           = Modifier.size(200.dp),
        )
```
Replace with:
```kotlin
        Image(
            painter            = painterResource(Res.drawable.logo),
            contentDescription = "Daniel & Nehemia logo",
            modifier           = Modifier.size(200.dp),
            colorFilter        = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        )
```

- [ ] **Step 3: LoginScreen — add import**

```kotlin
import androidx.compose.ui.graphics.ColorFilter
```

- [ ] **Step 4: LoginScreen — tint the logo**

Find:
```kotlin
        Image(
            painter            = painterResource(Res.drawable.logo),
            contentDescription = "Daniel & Nehemia logo",
            modifier           = Modifier.height(120.dp),
        )
```
Replace with:
```kotlin
        Image(
            painter            = painterResource(Res.drawable.logo),
            contentDescription = "Daniel & Nehemia logo",
            modifier           = Modifier.height(120.dp),
            colorFilter        = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        )
```

> Both files already import `androidx.compose.material3.MaterialTheme` (they use `MaterialTheme.colorScheme.background`). If a build error reports it missing, add it.

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/pending/screen/SplashScreen.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/login/screen/LoginScreen.kt
git commit -m "feat(theme): tint logo with onSurface (black light / white dark)"
```

---

## Task 8: Screen color audit — replace literals with tokens

Each file below references raw color constants or literals that do **not** flip in dark mode. Replace with `MaterialTheme.colorScheme.*`. Whites that sit on a `primary → primaryContainer` hero gradient become `onPrimary`; whites that are card/surface backgrounds become the matching surface token.

### 8a. HeroBannerSection.kt

**Files:** Modify `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/HeroBannerSection.kt`

- [ ] **Step 1: Remove the unused import**

Delete this line:
```kotlin
import com.hanmaum.dn.mobile.core.presentation.theme.LightPrimaryDark
```

- [ ] **Step 2: Replace each `LightPrimaryDark` usage** (lines ~84, 134, 164, 182) with `MaterialTheme.colorScheme.primaryContainer`. These are all inside `@Composable` scopes (gradient `colors = listOf(...)`, button `contentColor`, selected-dot color). Use Edit with `replace_all` on the token `LightPrimaryDark` → `MaterialTheme.colorScheme.primaryContainer`.

- [ ] **Step 3: Replace each `Color.White`** (lines ~94, 109, 118, 124, 187) with `MaterialTheme.colorScheme.onPrimary` — these are text/icon/overlay colors sitting on the terracotta gradient. (`Color.White.copy(alpha = 0.7f)` → `MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)`, etc. Preserve each existing `.copy(alpha = …)`.)

- [ ] **Step 4: Leave `GradientFadeBlack`** (`private val GradientFadeBlack = Color(0xFF1A0A0A)`) untouched — it is the intentional dark fade tail of the hero gradient and is mode-invariant by design.

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

### 8b. LatestNewsSection.kt

**Files:** Modify `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/LatestNewsSection.kt`

- [ ] **Step 6: Replace the card background** (line ~55)

Find: `colors = CardDefaults.cardColors(containerColor = Color.White),`
Replace: `colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),`

- [ ] **Step 7: Replace the grey fill** (line ~98)

Find: `color = Color(0xFFF5F5F5)`
Replace: `color = MaterialTheme.colorScheme.surfaceContainer`

### 8c. AnnouncementDetailScreen.kt

**Files:** Modify `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/AnnouncementDetailScreen.kt`

- [ ] **Step 8: Replace the four `Color.White` usages** (lines ~101, 106, 124, 132) — all on the `primary → primaryContainer` hero header — with `MaterialTheme.colorScheme.onPrimary`. Preserve `Color.White.copy(alpha = 0.25f)` → `MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)`.

### 8d. CalendarScreen.kt

**Files:** Modify `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarScreen.kt`

- [ ] **Step 9: Replace the two selected-segment `Color.White`** (lines ~97, 101) with `MaterialTheme.colorScheme.onPrimary` (white text sits on the primary-colored selected toggle).

### 8e. MinistryDetailScreen.kt

**Files:** Modify `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/presentation/detail/MinistryDetailScreen.kt`

- [ ] **Step 10: Replace the back-icon `tint = Color.White`** (line ~112) with `tint = MaterialTheme.colorScheme.onPrimary` (icon sits on the hero gradient).

- [ ] **Step 11: Verify the whole audit compiles**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 12: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/HeroBannerSection.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/LatestNewsSection.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/AnnouncementDetailScreen.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarScreen.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/presentation/detail/MinistryDetailScreen.kt
git commit -m "fix(theme): use colorScheme tokens so all screens flip in dark mode"
```

---

## Task 9: Full verification

- [ ] **Step 1: Run the unit tests**

Run: `./gradlew :composeApp:testDevDebugUnitTest`
Expected: PASS, including the new `ThemeRepositoryImplTest` (4) and `AppStringsTest`.

- [ ] **Step 2: Run Android Lint**

Run: `./gradlew lint`
Expected: No **new** errors. (Per `tasks/lessons.md`, 3 pre-existing geofence/notification Lint errors are known debt — not regressions.)

- [ ] **Step 3: Confirm no TODO in source** (CI gate)

Run: `grep -rn "TODO" composeApp/src || echo "no TODO — OK"`
Expected: `no TODO — OK` (or no matches).

- [ ] **Step 4: Build + launch iOS simulator, both modes** (per `tasks/lessons.md`; this Mac has Xcode 26.0.1)

```bash
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/dnbuild CODE_SIGNING_ALLOWED=NO
SIM=$(xcrun simctl list devices available | grep -oE '\([0-9A-F-]{36}\)' | tr -d '()' | head -1)
xcrun simctl boot "$SIM" 2>/dev/null; sleep 3
xcrun simctl install "$SIM" /tmp/dnbuild/Build/Products/Debug-iphonesimulator/HanmaumDnApp.app
# Light mode
xcrun simctl ui "$SIM" appearance light
xcrun simctl launch "$SIM" com.hanmaum.dn.mobile.HanmaumDnApp; sleep 3
xcrun simctl io "$SIM" screenshot /tmp/dn-light.png
# Dark mode
xcrun simctl ui "$SIM" appearance dark
xcrun simctl terminate "$SIM" com.hanmaum.dn.mobile.HanmaumDnApp
xcrun simctl launch "$SIM" com.hanmaum.dn.mobile.HanmaumDnApp; sleep 3
xcrun simctl io "$SIM" screenshot /tmp/dn-dark.png
```
Expected: app launches in both; `/tmp/dn-light.png` shows the **black** logo on cream, `/tmp/dn-dark.png` shows the **white** logo on espresso. Read both screenshots and confirm Splash/Login render correctly. (System-following confirms the `SYSTEM` path; the in-app Light/Dark override is verified by navigating to Profile → theme picker if a logged-in session is available, else covered by the unit test + `AppTheme` resolution logic.)

- [ ] **Step 5: Final acceptance check**

Confirm against the spec §6: theme is selectable + persisted (unit test green), logo inverts (screenshots), every audited screen reads tokens (build green, no remaining `LightXxx`/`Color.White`/literal in the audited files except the documented exceptions), pill nav intentionally unchanged.

- [ ] **Step 6: Push the branch (only when the user asks)**

```bash
git push -u origin feat/light-dark-mode
```
