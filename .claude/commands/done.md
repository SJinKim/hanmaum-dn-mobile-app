# Definition of Done (Mobile)

## Functional
- [ ] Feature works end-to-end on Android emulator against the real backend
- [ ] iOS build compiles (verify with Xcode or `./gradlew :composeApp:iosSimulatorArm64Test`)
- [ ] Unit tests passing for ViewModels and repositories
- [ ] No println, dead code, hardcoded URLs, or secrets
- [ ] All HTTP calls go through `createHttpClient(tokenStorage)` — no raw Ktor instances
- [ ] Screens follow designs/dn_app/DESIGN.md
- [ ] Navigation uses the type-safe `Routes.kt` objects — no string routes

## Build
- [ ] ./gradlew :composeApp:assembleDebug — no warnings
- [ ] ./gradlew :composeApp:allTests — all passing
- [ ] ./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon — clean

## Before Commit
- [ ] Update docs/superpowers/specs/ if the feature involves non-trivial architecture decisions
- [ ] Commit message follows convention: type(scope): summary
- [ ] Branch rebased on dev — no merge commits

## After completing a feature slice
Update the root MVP.md in dn-app:
- Mark app column ✅ for the affected feature row
- Never delete entries — status updates only
- Edit only the affected line
