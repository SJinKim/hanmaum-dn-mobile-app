---
name: debugging-ci-failures
description: Use when a GitHub Actions job fails in this repo (pr-check, distribute, TestFlight archive), when CI fails but local builds pass, or on errors like "PhaseScriptExecution failed exit 65", "ARCHIVE FAILED", "No profiles found", "framework not found", or a foojay/toolchain download error.
---

# Debugging CI Failures

## Overview

This repo's CI failures have burned multiple full days, and almost every hour
was lost to the same two mistakes: **debugging a masked error** and **firing
plausible theories instead of reading the real log**. The catalog below is the
distilled cost of those days — check it before forming any new theory.

## First moves (always, in order)

1. Get the real log: `gh run view <run-id> --log-failed` (find the id via
   `gh run list --workflow=<pr-check|distribute>.yml --limit 5`).
2. **If the failing step is an iOS archive with a generic
   `PhaseScriptExecution 'Compile Kotlin Framework' ... exit 65`: the real
   gradle error is hidden by xcpretty.** Get it from the matching `android-*`
   job on the same commit (plain gradle, unfiltered), or run the same gradle
   task locally. Never debug the xcpretty summary.
3. **If BOTH platforms fail on the same commit → shared Gradle/JVM/toolchain
   config is the cause.** It is not an Apple problem, not a signing problem,
   not a version-number problem.
4. Only then form a theory — and check the catalog first.

## Known-failure catalog (symptom → real cause → fix)

| Symptom in log | Real cause | Fix |
|---|---|---|
| `Unable to download toolchain ... api.foojay.io ... 400` | `gradle/gradle-daemon-jvm.properties` pins a vendor/version CI can't get | Delete the file; never commit `updateDaemonJvm` output |
| `ld: framework '_LocationEssentials' not found` at a simulator link | Kotlin/Native 2.3.0's bundled CoreLocation auto-links a private framework absent from the simulator SDK (KT-71566) | The `.tbd` stub block in `composeApp/build.gradle.kts` handles it — simulator target ONLY, never device |
| `No Accounts: Add a new account` during archive | gym does NOT forward the ASC API key to `xcodebuild archive` | Pass `-authenticationKeyPath/-KeyID/-IssuerID` via `build_app(xcargs:)` |
| `profile doesn't match ... application-groups` | Entitlement requests an App Group not registered in the portal | Remove the entitlement (it's deferred for the future widget) or register the group first |
| `Revoke certificate: private key is not installed` / `No profiles found` | Automatic signing on ephemeral runners orphans a Development cert per run | fastlane match only (PR #64); never revert to automatic signing in CI |
| Upload `Validation failed (409) ... must be built with the iOS 26 SDK` | Runner's Xcode too old | `runs-on: macos-15` + `setup-xcode latest-stable` — keep ios-check on the same |
| `Failed to build cache ... getBackStackEntry ... [inline] is not found` | Xcode 26 toolchain crashes the K/N static-cache builder on navigation-compose | `kotlin.native.cacheKind.ios*=none` in `gradle.properties` (already set — don't remove) |
| iOS launches then crashes: `IrLinkageError ... $stable ... backing field` | lifecycle / navigation-compose / koin compiled against different lifecycle lines | Align all three (check the candidate's `.module` on Maven); reproduce on the local simulator, not TestFlight |
| `Cannot locate tasks that match ':composeApp:assembleReleaseX64'` in ios-check | A whole workflow file was copied from another branch to pick up one rule, replacing a tuned job with an older broken one | Copy the *hunk*, never the file. The job needs `macos-15`, `setup-xcode latest-stable`, and the `xcodebuild` Swift-interop step |
| `ios-check` red, `android-common-check` green | Real iOS regression — the gate has been trustworthy since the stub fix | Reproduce locally with `DEVELOPER_DIR=... :composeApp:iosSimulatorArm64Test` + the xcodebuild interop build |

## Theories that already wasted days — do not revisit

- "Apple rejects the marketing-version downgrade" — version/build validation
  happens only at upload, and a lower marketing version is accepted. A
  build-step failure is never an Apple/version issue.
- "It's the Xcode/SDK version" for a link error — `_LocationEssentials` is
  baked into Kotlin/Native's platform lib, independent of Xcode.
- "Poisoned gradle cache / flaky runner — just retrigger" — CI-vs-local splits
  here have always been real config differences (toolchain file, missing .env
  value, JDK vendor). Diff the environments instead.

## Stop rule

Two distinct fix attempts failed → stop pushing commits at CI. Write down:
failing step, exact error line, what both attempts assumed. Reproduce the
failing gradle/xcodebuild task locally (this Mac has full Xcode at
`/Applications/Xcode.app`; prefix with `DEVELOPER_DIR=`). Each blind CI push
costs ~8 min (pr-check) to ~15 min (archive) — local reproduction is almost
always faster by the third attempt.

## After it's fixed

Append the new symptom → cause → fix row to this catalog and to
`tasks/lessons.md` in the same PR. The catalog is only valuable if it stays
complete.
