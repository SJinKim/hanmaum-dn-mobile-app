---
name: mobile-quality-gate
description: Use before declaring work done, before a PR, or when asked to review mobile app changes. Runs the repo's Definition of Done, review checklist, Gradle checks, and mobile/KMP risk scan.
---

# Mobile Quality Gate

Use this skill for final verification and PR readiness.

## Checklist

1. Inspect `git diff --stat` and changed files.
2. Review against `docs/codex/code-review.md`.
3. Ensure tests exist for behavior changes.
4. Run relevant checks:
   - targeted tests first
   - `./gradlew :composeApp:testDevDebugUnitTest`
   - `grep -rn "TODO" composeApp/src` — must print nothing (CI fails on any match)
   - `./gradlew lint` — 0 errors
   - `./gradlew :composeApp:assembleDevDebug` for Android-impacting changes
   - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test`
     for shared/iOS-impacting changes (the prefix is required — without it the task
     dies with xcrun exit 72)

   Bare task names are wrong in this repo: product flavors make `assembleDebug` /
   `testDebugUnitTest` / `compileDebugKotlinAndroid` ambiguous, and `allTests` fails
   at the iOS native link without full Xcode.
5. Scan for secrets, hardcoded URLs, TODO/FIXME, raw `HttpClient`, string routes, and auth-token leakage.

## Output

Return PASS/FAIL, blocking issues, commands run, results, and residual risk.
