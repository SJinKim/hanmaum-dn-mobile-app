# Commit & Push Convention

## Format
<type>(<scope>): <summary max 72 chars>

Types: feat | fix | refactor | test | chore | docs | perf | revert
- Imperative mood ("add" not "added"), no period
- Body: explain WHY, not WHAT
- Reference issues: closes #42

## Pre-Commit Checklist
1. ./gradlew :composeApp:allTests
2. ./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon
3. ./gradlew :composeApp:assembleDebug
4. Confirm no secrets, no hardcoded URLs (use BuildKonfig), no commented-out code
