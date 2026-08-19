# PR Review Command — Pre-PR Quality Gate

Run a full quality pass before push/PR:

1. `git diff --stat` and inspect changed files.
2. Review against `docs/codex/code-review.md`.
3. Run `./gradlew :composeApp:testDevDebugUnitTest`.
4. Run `grep -rn "TODO" composeApp/src` — must print nothing (CI fails on any match).
5. Run `./gradlew lint` — 0 errors; read the count from
   `composeApp/build/reports/lint-results-devDebug.xml`, never from a doc.
6. Run `./gradlew :composeApp:assembleDevDebug` when Android is affected.
7. When shared/iOS-sensitive code is affected:
   `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test`
   — and add the `xcodebuild` Swift interop build from AGENTS.md if a Kotlin
   declaration Swift calls changed.
7. Check introduced TODO/FIXME, hardcoded URLs, secrets, raw `HttpClient`, and string navigation routes.
8. For UI changes, verify `designs/dn_app/DESIGN.md` token usage.
9. Output PASS/FAIL with blocking issues and exact verification results.
