# PR Review Command — Pre-PR Quality Gate

Run a full quality pass before push/PR:

1. `git diff --stat` and inspect changed files.
2. Review against `docs/codex/code-review.md`.
3. Run `./gradlew :composeApp:allTests`.
4. Run `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon`.
5. Run `./gradlew :composeApp:assembleDebug` when Android is affected.
6. Run `./gradlew :composeApp:iosSimulatorArm64Test` when shared/iOS-sensitive code is affected.
7. Check introduced TODO/FIXME, hardcoded URLs, secrets, raw `HttpClient`, and string navigation routes.
8. For UI changes, verify `designs/dn_app/DESIGN.md` token usage.
9. Output PASS/FAIL with blocking issues and exact verification results.
