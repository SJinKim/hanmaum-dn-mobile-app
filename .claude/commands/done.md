# Definition of Done (Mobile)

Run the full verification ladder from the `verifying-kmp-changes` skill —
it is the single source of truth for which gates a change needs and the exact
commands (flavored task names, `DEVELOPER_DIR` prefix for iOS). Then confirm:

## Functional
- [ ] Feature works end-to-end on the Android emulator against the real backend
- [ ] iOS gates for the change type passed (native tests / Swift interop build /
      simulator launch — per the skill's gate table)
- [ ] Unit tests passing for ViewModels and repositories (`:composeApp:testDevDebugUnitTest`)
- [ ] No println, dead code, hardcoded URLs (BuildKonfig only), or secrets
- [ ] All HTTP calls go through `createHttpClient(tokenStorage)` — no raw Ktor instances
- [ ] Every user-visible string in `AppStrings` + KO + EN + DE
- [ ] Screens follow designs/dn_app/DESIGN.md (tokens only, no 1px dividers, spring motion)
- [ ] New screens: lazyweb references consulted first (`lazyweb:lazyweb-quick-references`);
      significant redesigns: `lazyweb:lazyweb-design-improve` with a current screenshot
- [ ] Navigation uses type-safe `Routes.kt` objects — no string routes

## Build gates (evidence shown, not claimed)
- [ ] `./gradlew :composeApp:testDevDebugUnitTest` — all passing
- [ ] `grep -rn "TODO" composeApp/src` — no matches (CI fails on any)
- [ ] `./gradlew lint` — 0 errors (baseline is clean; any error is yours)
- [ ] Shared/iOS-reachable code:
      `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test`
- [ ] Android build: `./gradlew :composeApp:assembleDevDebug`

## Before commit
- [ ] Non-trivial architecture → spec/plan updated in `docs/superpowers/`
- [ ] Commit message: `type(scope): summary` (≤72 chars, imperative, no AI trailers)
- [ ] Branch is `feature/<name>` rebased on `develop` — no merge commits, never on develop/main

## After completing a feature slice
Update `../hanmaum-dn-ops/docs/MVP.md`:
- Mark the app column ✅ for the affected feature row
- Never delete entries — status updates only
- Edit only the affected line
