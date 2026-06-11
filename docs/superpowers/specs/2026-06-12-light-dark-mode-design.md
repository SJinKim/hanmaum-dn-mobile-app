# Design: App-wide Light / Dark Mode

**Date:** 2026-06-12
**Branch:** `fix/ios-irlinkage-version-align` (work rolls into this branch alongside the `.gitignore` change)
**Status:** Approved — ready for implementation plan

## 1. Goal

Give the app a first-class light and dark mode:

1. A user-selectable theme: **System / Light / Dark**, persisted across launches.
2. The in-app DN logo renders **black in light mode, white in dark mode**.
3. The full Warm-Premium palette is correctly applied to **every screen**, so all fields, surfaces, and visuals flip to the correct mode (no stale hardcoded colors).

DESIGN.md (`designs/dn_app/DESIGN.md`) is the single source of truth for all color, surface, typography, shape, and motion tokens. No external design research is used — both light and dark palettes are already defined there and in code.

## 2. Current State (what already exists)

- `core/presentation/theme/AppTheme.kt` already wraps the app and switches palettes via `isSystemInDarkTheme()`.
- `core/presentation/theme/AppColors.kt` already defines **both** complete schemes: `WarmPremiumLightColorScheme` and `WarmPremiumDarkColorScheme`, matching DESIGN.md §2 tokens.
- `App.kt` already hoists **locale** state with a clean, established pattern:
  - `LocaleRepository` (settings-backed) + `var locale by remember { mutableStateOf(localeRepo.getLocale()) }`
  - passed to `ProfileScreen` as `currentLocale` / `onLocaleChange`, persisted via `localeRepo.setLocale(...)`.
- `multiplatform-settings` is already wired (`TokenStorageImpl` uses a shared `Settings` instance from Koin).
- Logo: a single `composeApp/src/commonMain/composeResources/drawable/logo.png` — 1200×1200 **RGBA with a fully transparent background and opaque black glyphs** (verified: corner pixel `(0,0,0,0)`). Used on Splash + Login. Fully tintable.

**The core gap:** theme cannot be chosen in-app (system-only today), the logo does not adapt, and some screens reference the raw `LightXxx` color constants directly instead of `MaterialTheme.colorScheme.*`, so those colors never flip in dark mode.

## 3. Approach

Mirror the **existing locale pattern** exactly. The locale feature already solved "user-selectable, persisted, hoisted-in-App, set-from-Profile" — reusing that shape keeps the two settings consistent and minimizes new machinery. Rejected alternatives: a `CompositionLocal` theme controller or a dedicated `ThemeViewModel` — both add structure the locale feature deliberately did not need.

## 4. Components

### 4.1 Theme state & persistence
- **`ThemeMode` enum** — `SYSTEM`, `LIGHT`, `DARK`. Location: `core/domain/model/ThemeMode.kt` (alongside `NavRoute`).
- **`ThemeRepository`** interface — `core/domain/repository/ThemeRepository.kt`:
  - `fun getThemeMode(): ThemeMode` (default `SYSTEM`)
  - `fun setThemeMode(mode: ThemeMode)`
- **`ThemeRepositoryImpl`** — `core/data/repository/ThemeRepositoryImpl.kt`, backed by the same injected `Settings`. Persists the enum name under a `"theme_mode"` key; unknown/missing value falls back to `SYSTEM`.
- **DI:** bind `ThemeRepository` → `ThemeRepositoryImpl` in `di/AppModule.kt`, next to `LocaleRepository`.

### 4.2 Theme resolution
- `AppTheme` gains a `themeMode: ThemeMode` parameter:
  ```kotlin
  val dark = when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT  -> false
      ThemeMode.DARK   -> true
  }
  val colorScheme = if (dark) WarmPremiumDarkColorScheme else WarmPremiumLightColorScheme
  ```
- No palette/token changes — both schemes already exist and are final per DESIGN.md.

### 4.3 App.kt wiring
- Inject `ThemeRepository`; hoist `var themeMode by remember { mutableStateOf(themeRepo.getThemeMode()) }`.
- Call `AppTheme(themeMode = themeMode) { ... }`.
- Pass `currentTheme = themeMode` / `onThemeChange = { themeRepo.setThemeMode(it); themeMode = it }` into `ProfileScreen` — identical to the existing locale plumbing.

### 4.4 Toggle UI (in Profile)
- The theme picker lives in `ProfileScreen`, where the locale picker already lives — **no new Settings screen**.
- It **mirrors the existing language picker exactly** (the "mirror the locale pattern" principle, §3): a tappable `Card` in the "Account Preferences" section showing the current mode, which opens a `ModalBottomSheet` (`ThemePickerSheet`) listing System / Light / Dark. The selected row is marked with `primary` text + a `Check` icon, exactly like `LanguagePickerSheet`. This keeps the two settings visually and structurally identical and reuses the already-themed Material3 sheet (which inherits DESIGN.md tokens via `MaterialTheme`).
- Labels are localized through the existing `LocalStrings` mechanism (add `profileTheme`, `selectTheme`, `themeSystem`, `themeLight`, `themeDark` to `AppStrings` + `EnStrings`, `KoStrings`, `DeStrings`).

### 4.5 Logo tinting
- Wrap the logo `Image` on **Splash** (`SplashScreen.kt`) and **Login** (`LoginScreen.kt`) with `colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)`.
- Result: `onSurface` = espresso `#2C1A0E` in light (reads black) and cream `#F5E6CC` in dark (reads white) — satisfies the requirement while honoring DESIGN.md's no-pure-black/white rule. Transparent areas stay transparent.
- **Out of scope:** OS launcher / app icons (Android adaptive icon, iOS app icon) — these are OS-fixed and already set in branding PR #59; they are not theme-switchable in the same way.

### 4.6 Screen color audit
Replace direct references to the raw `LightXxx` / `DarkXxx` constants (and any hardcoded `Color(0x…)` / `Color.White` / `Color.Black` that should adapt) with `MaterialTheme.colorScheme.*` tokens so every screen flips correctly.

Primary targets identified by survey (files referencing raw constants or hardcoded literals):
- `features/announcement/presentation/components/HeroBannerSection.kt`
- `features/announcement/presentation/components/LatestNewsSection.kt`
- `features/announcement/presentation/AnnouncementDetailScreen.kt`
- `features/calendar/presentation/CalendarScreen.kt`
- `features/ministry/presentation/detail/MinistryDetailScreen.kt`
- `features/login/presentation/RegisterScreen.kt`

Plus a confirmation sweep across all 17 screens (Home, AnnouncementList, Albums, AlbumDetail, PhotoViewer, Attendance, FloorPlan, CommunityStub, MinistryList, Profile, Pending, Splash, Login) to ensure each reads tokens, not literals.

**Intentional exception:** the floating pill nav (`FloatingPillNav`) stays dark-frosted in **both** modes by design (DESIGN.md §6) — its `PillBackground` / `PillIndicator` / `PillIcon*` constants are deliberately mode-invariant and must **not** be converted to flipping tokens.

**Mapping guidance** (raw constant → token):
- `LightSurface` / `DarkSurface` → `colorScheme.surface`
- `LightSurfaceContainerLowest` → `colorScheme.surfaceContainerLowest` (cards)
- `LightSurfaceContainerLow` → `colorScheme.surfaceContainerLow`
- `LightSurfaceContainer` → `colorScheme.surfaceContainer`
- `LightOnSurface` → `colorScheme.onSurface`; `LightOnSurfaceVariant` → `colorScheme.onSurfaceVariant`
- `LightMuted` → `colorScheme.outline` (the scheme maps `outline = …Muted`)
- `LightPrimary` → `colorScheme.primary`; `LightPrimaryDark` → `colorScheme.primaryContainer`; `LightOnPrimary` → `colorScheme.onPrimary`
- Hero gradient: build the `135°` brush from `colorScheme.primary` → `colorScheme.primaryContainer` so it adapts.

## 5. Out of Scope (YAGNI)

- No new Settings screen (toggle lives in Profile).
- No second logo asset (single asset is tinted).
- No changes to OS launcher/app icons.
- No changes to the defined palette tokens (CI-pending primary/secondary stay as placeholders per DESIGN.md).
- No automatic per-screen scheduled theme (e.g. time-of-day) — only System/Light/Dark.

## 6. Verification

1. `./gradlew :composeApp:allTests` — JVM/Android common tests pass (iOS native link step is not validatable with CLT-only per lessons.md; full Xcode present locally).
2. `./gradlew lint` — no new Android Lint errors (3 pre-existing geofence/notification errors are known debt, not regressions). No `TODO` in `composeApp/src`.
3. iOS simulator (Xcode 26.0.1 local) build + launch; cycle System/Light/Dark in Profile and confirm persistence across relaunch.
4. Screenshots in **both** light and dark: Splash, Login, Home, Profile, and one detail screen — confirm logo inverts and no element keeps a stale (non-flipping) color.
5. A staff engineer would approve: theme is selectable + persisted, logo adapts, every screen reads tokens, pill nav intentionally unchanged.

## 7. Risks / Notes

- **Pill nav must not flip** — easy to over-correct during the audit. Leave `Pill*` constants alone.
- **iOS runtime** — per lessons.md, reproduce/verify on the local simulator rather than TestFlight.
- The `.gitignore` change for `iosApp/Gemfile.lock` is committed together with this work (per user request to roll it in).
