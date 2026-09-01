---
name: verifying-kmp-changes
description: Use when about to claim a change in this repo is done, before committing, pushing, or opening a PR — or when unsure which Gradle/Xcode checks a given change actually needs. Also use when a "standard" Gradle task name fails (assembleDebug, testDebugUnitTest, allTests, xcrun exit 72).
---

# Verifying KMP Changes

## Overview

This repo has product flavors, an iOS toolchain quirk, and a Swift target that
gradle never compiles — so the "obvious" verification commands are all wrong.
This skill is the exact ladder. **A claim of "done" without the ladder's output
is a false claim.**

Baseline failures this prevents (all real, see `tasks/lessons.md`): bare
`assembleDebug`/`testDebugUnitTest` failing on flavor ambiguity, `allTests`
dying at the iOS link on this machine, Swift↔Kotlin interop breaks surfacing
only in a 15-min post-merge TestFlight archive, and "fixed" bugs that were
never run.

## Which gates does my change need?

| Change touches | Required gates |
|---|---|
| Anything at all | 1, 2, 3 |
| `commonMain` / `iosMain` / anything iOS-reachable | + 4 |
| A Kotlin declaration Swift calls (`MainViewController`, `KoinHelper`, framework API) | + 5 |
| UI / navigation / DI wiring / auth-token flow | + 6 (run the app, drive the flow) |
| `buildkonfig {}`, `.env` fields, flavors | + 7 |

## The ladder

```bash
# 1. Unit tests — flavored name, never testDebugUnitTest
./gradlew :composeApp:testDevDebugUnitTest

# 2. TODO gate — CI fails on an UNREFERENCED TODO only. One that names its
#    tracking issue, e.g. TODO(hanmaum-dn-server#115), is deliberate and stays.
#    This is the exact grep from pr-check.yml — never "fix" it by deleting a
#    referenced TODO.
grep -rn "TODO" composeApp/src | grep -v "TODO("   # must print nothing

# 3. Lint — there is NO ktlint/spotless/detekt; this is the only lint gate.
#    Baseline is 0 errors — any error is yours, and it fails the build
#    (abortOnError defaults true; there is no lint-baseline.xml). Read the
#    report, never a remembered number:
#    grep -c 'severity="Error"' composeApp/build/reports/lint-results-devDebug.xml
./gradlew lint

# 4. iOS native tests — xcode-select points at CommandLineTools, so every iOS
#    task needs DEVELOPER_DIR or it dies with xcrun exit 72
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  ./gradlew :composeApp:iosSimulatorArm64Test

# 5. Swift interop gate — gradle link tasks never compile the Swift target.
#    Catches dropped Kotlin default params and init→doInit renames.
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild build \
  -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO

# 6. Run it — Android: assembleDevDebug + emulator against real backend.
#    iOS simulator (fastest way to see a Kotlin crash + screenshot):
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/dnbuild CODE_SIGNING_ALLOWED=NO
SIM=$(xcrun simctl list devices available | grep -oE '\([0-9A-F-]{36}\)' | tr -d '()' | head -1)
xcrun simctl boot "$SIM" 2>/dev/null
xcrun simctl install "$SIM" /tmp/dnbuild/Build/Products/Debug-iphonesimulator/HanmaumDnApp.app
xcrun simctl launch --console-pty "$SIM" com.hanmaum.dn.mobile.HanmaumDnApp
xcrun simctl io "$SIM" screenshot /tmp/dn-verify.png

# 7. BuildKonfig changes — verify the generated actuals, don't trust the DSL
./gradlew :composeApp:generateBuildKonfig --rerun-tasks
# then inspect composeApp/build/buildkonfig/<target>Main/.../BuildKonfig.kt
# and confirm the delegatingSource string in composeApp/build.gradle.kts
# lists every field (the doLast hook rewrites the Android actual).
```

## Rules

- **Evidence before claims.** Paste the failing/passing output; never report a
  gate you didn't run. If a gate can't run, name it and the residual risk.
- **A compile is not a verification.** Gate 6 exists because DI wiring, routes,
  and token flow only break at runtime.
- **iOS is not optional** for `commonMain` changes — Android tolerating
  something (version skew, test names with punctuation) proves nothing about
  Kotlin/Native.

## Red flags — stop, you're rationalizing

- "It's a trivial change, tests are overkill" — the flavored-task and
  token-leak bugs were both 'trivial' changes.
- "allTests covers everything" — it link-fails locally; use gates 1+4.
- "The gradle build passed, so iOS is fine" — gate 5 exists because it isn't.
- "I'll let CI catch it" — one CI cycle ≈ 8 min, a TestFlight archive ≈ 15;
  the ladder is faster.
