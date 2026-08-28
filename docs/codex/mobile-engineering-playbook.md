# Mobile Engineering Playbook — hanmaum-dn-mobile-app

This playbook captures how a senior mobile engineer should think while building this KMP Android/iOS app with Codex.

## Priority 0 — Product and Risk Framing

Before coding, identify:

- User journey affected: auth, approval, home, news, calendar, album, attendance, profile, etc.
- Platforms affected: Android only, iOS only, or shared KMP.
- Risk class: auth/security, networking, offline/cache, permissions/location, UI-only, or build/config.
- Verification path: unit test, repository test, ViewModel test, Android APK, iOS simulator, visual check.

Prefer the smallest feature slice that can be proven correct.

## Priority 1 — KMP Boundaries

- Put reusable business logic, models, repositories, ViewModels, and Compose UI in `commonMain` when they compile cleanly on both Android and iOS.
- Keep Android-specific APIs (`Context`, Google Play Services, Firebase Android APIs, Android permissions) in `androidMain` or behind abstractions.
- Keep iOS-specific APIs in `iosMain` and expose them through common interfaces.
- Do not reference JVM/Android APIs from `commonMain` unless the library is truly multiplatform.
- If a feature needs platform behavior, design the common interface first and bind implementations in Koin.

## Priority 2 — Architecture and State

- UI state should be immutable data classes or sealed states.
- ViewModels should own loading, error, retry, and one-shot navigation/event behavior.
- Screens should be mostly stateless render functions plus event callbacks.
- Repositories should hide data sources and expose domain models.
- DTO-to-domain mapping belongs in data layer, not in composables.
- Navigation uses route objects from `Routes.kt`; do not introduce string routes.

## Priority 3 — Networking and Data

- Use the shared Ktor client and existing repository pattern.
- Treat backend API responses as unstable at boundaries: validate nullable fields and map safely.
- Never send backend bearer tokens to third-party hosts.
- Avoid hidden global state. Prefer injected repositories/storage/services.
- Caching must define its source of truth and invalidation behavior.

## Priority 4 — Compose Multiplatform UI

- Start from `designs/dn_app/DESIGN.md` and existing screens/components.
- Prefer reusable components in `core/presentation/components/` when a pattern repeats.
- Avoid expensive sorting/filtering/date formatting inside composables; move to ViewModel or memoize with `remember`.
- Lazy lists need stable keys when items can be inserted, removed, or reordered.
- Use `derivedStateOf` only for derived state that changes more often than the UI needs to recompose.
- Keep preview/debug-only code out of production paths.

## Priority 5 — iOS and Android Quality

Android:

- Verify APK build for Android-impacting changes.
- Respect runtime permissions and lifecycle.
- Avoid Context leaks and long-running main-thread work.

IOS:

- Compile shared code for iOS simulator when shared code changes.
- Avoid assumptions from Android UI/navigation behavior that may not hold on iOS.
- For performance-sensitive changes, prefer device profiling over simulator assumptions.

## Priority 6 — Tests

- ViewModel behavior: fake repositories + `kotlinx.coroutines.test`.
- Repository behavior: Ktor `MockEngine` where practical.
- Pure domain logic: common tests.
- Navigation/top-level config: cheap common tests.
- Regression bugs: add a failing test first when feasible, then fix.

## Priority 7 — Staff-Engineer Review Questions

Ask before finishing:

- Did I solve the root cause with minimal blast radius?
- Does this compile for every target touched?
- Can a future developer understand the change without asking me?
- Are auth, secrets, permissions, and third-party calls safe?
- Are loading, empty, error, retry, and success states handled?
- Does UI follow `DESIGN.md` instead of ad-hoc styling?
- Are tests meaningful, not just coverage theater?
