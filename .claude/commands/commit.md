# Commit & Push Convention

## Format
<type>(<scope>): <summary max 72 chars>

Types: feat | fix | refactor | test | chore | docs | perf | revert
- Imperative mood ("add" not "added"), no period
- Body: explain WHY, not WHAT
- Reference issues: closes #42

## Pre-Commit Checklist
Run the gates the `verifying-kmp-changes` skill selects for this change — it is
the single source of truth for which gates apply and the exact commands. The
minimum for any change:

1. `./gradlew :composeApp:testDevDebugUnitTest`
2. `grep -rn "TODO" composeApp/src` — must print nothing (CI fails on any match)
3. `./gradlew lint` — 0 errors
4. `./gradlew :composeApp:assembleDevDebug`
5. Touched `commonMain`/`iosMain`? Add the iOS gates from the skill
   (`DEVELOPER_DIR=...` prefix, or they die with xcrun exit 72)
6. Confirm no secrets, no hardcoded URLs (use BuildKonfig), no commented-out code

Flavors (`dev`/`st`/`prod`) make bare task names wrong: never `allTests`,
`assembleDebug`, `compileDebugKotlinAndroid`, or `testDebugUnitTest`.
