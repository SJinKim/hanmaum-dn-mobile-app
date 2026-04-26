Your goal is to run a full pre-PR quality check before pushing a branch.

1. Run `./gradlew :composeApp:allTests` — all tests must pass
2. Run `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon` — must succeed with no errors
3. Run `./gradlew :composeApp:assembleDebug` — debug APK must build
4. Check for any TODO or FIXME comments introduced in this branch's changed files
5. Check no secrets or hardcoded URLs were added — all environment URLs must come from BuildKonfig
6. Verify new screens use type-safe routes from `core/navigation/Routes.kt` (no string routes)
7. Verify new HTTP calls go through the shared Ktor client (no ad-hoc HttpClient instances)
8. Spot-check that new UI matches designs/dn_app/DESIGN.md
9. Output a final PASS / FAIL summary with a list of any blocking issues
