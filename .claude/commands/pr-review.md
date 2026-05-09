Your goal is to run a full pre-PR quality check before pushing a branch.

1. Run `./gradlew :composeApp:allTests` — all tests must pass
2. Run `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon` — must succeed with no errors
3. Run `./gradlew :composeApp:assembleDebug` — debug APK must build
4. Check for any TODO or FIXME comments introduced in this branch's changed files
5. Check no secrets or hardcoded URLs were added — all environment URLs must come from BuildKonfig
6. Verify new screens use type-safe routes from `core/navigation/Routes.kt` (no string routes)
7. Verify new HTTP calls go through the shared Ktor client (no ad-hoc HttpClient instances)
8. Spot-check that new UI matches designs/dn_app/DESIGN.md — color tokens, typography, surface layering, no 1 px borders, pill/rounded shapes
9. For new screens: check `.lazyweb/` exists with reference images, confirming design research was run (invoke `lazyweb:lazyweb-quick-references` if missing)
10. For modified screens: check that the screen visually matches other existing screens in the app (same corner radius, spacing, and surface tones)
11. Output a final PASS / FAIL summary with a list of any blocking issues
