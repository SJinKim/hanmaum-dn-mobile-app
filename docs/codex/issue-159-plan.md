# Issue #159 — initial language choice and consistent localization

1. Inspect existing locale persistence, startup, settings and localized screens; correct the issue examples to 성: 홍 / 이름: 길동. Preserve all user changes in the original checkout.
2. Extend the existing locale repository to distinguish an explicit KO/EN selection from its fallback; expose language state through a shared ViewModel. Show a bilingual first-run picker before authentication; preserve language across restart/logout and navigation across settings changes. Migrate unsupported stored locales safely.
3. Add persistent localized Hangul guidance and 홍 / 길동 examples to registration. Audit app-owned UI and status/date formatting for KO/EN consistency, retaining backend/user content unchanged. Add focused regression tests.
4. Run targeted tests, all JVM tests, TODO gate, lint, Android APK and iOS native tests; run Swift interop if exported signatures change. Review the diff and report remaining device-only verification.

## Design

Use existing shared scaffolding, surfaces, typography and spacing. The first-run picker has one static bilingual presentation, with two immediately actionable language choices; it needs no loading or intermediate selection states. Lazyweb tools are unavailable. Existing screens retain their layouts.

## Boundaries

No new dependencies, server/schema changes, account-synced locale or automatic translation of backend/user content. No simulator launch or TestFlight distribution.

## Verification and review

- Focused locale, language-state, localization and registration tests passed before the full suite.
- `./gradlew :composeApp:testDevDebugUnitTest`: passed, 701 tests, 0 failures/errors.
- `./gradlew lint`: passed, 0 errors in `lint-results-devDebug.xml` (53 warnings).
- `./gradlew :composeApp:assembleDevDebug`: passed.
- TODO gate: no unreferenced TODOs. `git diff --check`: clean.
- `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test`: passed, 696 tests, 0 failures/errors.
- Swift entrypoints (`MainViewController`, Koin initialization) are unchanged; no Swift-called declaration changed.

Source review covers auth/registration/pending, home/news, attendance/calendar/RSVP, profile/settings, albums/community and the remaining ministry/training/floor-plan/notification screens. App-owned labels use the central strings; visible transient errors use typed messages resolved at render time. Dates/counts respect KO/EN. Backend/user content, including names and editorial text, remains unchanged. A JVM source guard rejects new hardcoded Korean literals outside the localization layer; translation coverage tests check both languages.

Manual visual/accessibility checks on Android and iOS, including large text and light/dark appearance, still require device testing. iOS runtime verification belongs to the user on TestFlight. No simulator app was launched and no distribution was dispatched.

## Final review correction

The Material birthday picker had a separate device-locale default. Use a small expect/actual locale factory (Android Locale / iOS NSLocale), construct its state with the explicit app locale, and localize its title and date formatter/headline. Preserve the existing calendar and typed input. Add cross-platform tests with opposing app/device formatter locales, then rerun the gates for the final code.

Also provide localized Android configuration/context and the iOS AppleLanguages override for library-owned Material labels and accessibility descriptions. Keep the composition identity stable (no locale key), so settings changes preserve navigation. This follows [JetBrains' locale environment approach](https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html); platform locale handling stays behind expect/actual.

## PR #289 review follow-up

1. Preserve the registration endpoint's duplicate-email conflict as a typed domain failure. Render actionable KO/EN guidance, retaining generic unknown-error and 5xx handling.
2. Cover repository classification (including empty/non-JSON bodies) and ViewModel state with regression tests; rerun focused and full mobile gates.
3. Push the correction, wait for all CI checks on the final revision, merge #289, and remove only its remote branch after checking stacked PRs.

The final lint run exposed Android's `AppBundleLocaleChanges` warning: Material supplies Android XML translations, and Play language splits would otherwise install only the device language. Disable language splitting in the base bundle so the initial picker works independently of that language. Verify lint, APK and a development bundle's configuration in addition to the normal gates. This follows [Android's guidance for in-app language pickers](https://developer.android.com/guide/app-bundle/configure-base#handling_language_changes).

Final follow-up verification: all 701 JVM tests and all 696 native iOS tests passed. Lint returned 0 errors and 53 warnings; the language-split warning is gone. `assembleDevDebug` and `bundleDevDebug` passed, and the generated AAB's `BundleConfig.pb` explicitly disables the LANGUAGE split. The TODO gate and whitespace checks are clean. No Swift-called declaration changed.
