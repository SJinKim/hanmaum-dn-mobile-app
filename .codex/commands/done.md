# Done Command — Definition of Done

Before saying Done:

- [ ] Tests added/updated for behavior changes.
- [ ] Relevant targeted tests pass.
- [ ] `./gradlew :composeApp:testDevDebugUnitTest` passes (output shown).
- [ ] `grep -rn "TODO" composeApp/src` prints nothing (CI fails on any match).
- [ ] `./gradlew lint` — 0 errors.
- [ ] `./gradlew :composeApp:assembleDevDebug` when Android is affected.
- [ ] Shared/iOS-reachable code: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test`; add the Swift interop build if a Kotlin API Swift calls changed.
- [ ] Diff reviewed for architecture drift, auth leaks, secrets, hardcoded URLs, TODO/FIXME, and dead code.
- [ ] UI changes follow `designs/dn_app/DESIGN.md`.
- [ ] Final response lists changed files, test results, and residual risk.
