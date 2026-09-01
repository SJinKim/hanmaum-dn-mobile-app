Your goal is to run a full pre-PR quality check before pushing a branch.
The exact commands and known baselines live in the `verifying-kmp-changes`
skill — use it; do not fall back to bare task names (`assembleDebug`,
`testDebugUnitTest`, `allTests` are all wrong in this repo).

1. `./gradlew :composeApp:testDevDebugUnitTest` — all tests must pass
2. `grep -rn "TODO" composeApp/src | grep -v "TODO("` must print nothing — CI fails
   on an unreferenced TODO only; `TODO(hanmaum-dn-server#115)` is allowed and stays.
   Also check no FIXME introduced in this branch's changed files
3. `./gradlew lint` — 0 errors (baseline is clean; any error is yours, and it fails the build)
4. `./gradlew :composeApp:assembleDevDebug` — debug APK must build
5. If the branch touches `commonMain`/`iosMain` or anything iOS-reachable:
   `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test`
   — and if a Kotlin declaration Swift calls changed, run the xcodebuild
   simulator interop build from the skill
6. Check no secrets or hardcoded URLs were added — all environment URLs come from
   BuildKonfig; if a config field was added, verify all five wiring places (CLAUDE.md §4)
7. Verify new screens use type-safe routes from `core/navigation/Routes.kt` (no string routes)
8. Verify new HTTP calls go through the shared Ktor client (no ad-hoc HttpClient instances,
   no bearer token leaving for foreign hosts)
9. Verify every new user-visible string exists in `AppStrings` + Ko/En/De implementations
10. Spot-check new UI against designs/dn_app/DESIGN.md — tokens only, surface layering,
    no 1px borders, spring-only motion, pill shapes, 80dp bottom padding on tab screens
11. For new screens: check `.lazyweb/` has reference images for this screen, confirming
    design research was run (invoke `lazyweb:lazyweb-quick-references` if missing)
12. For modified screens: confirm visual consistency with sibling screens (corner radius,
    spacing, surface tones)
13. Output a final PASS / FAIL summary with a list of any blocking issues, with the
    actual command output as evidence — never report a gate you did not run
