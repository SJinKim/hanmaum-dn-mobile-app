# CLAUDE.md — Operating Manual for hanmaum-dn-mobile-app

This file is the binding operating manual for AI work in this repo. When a rule here
conflicts with your instinct, the rule wins. When reality conflicts with this file,
say so out loud and fix the file in the same PR.

## 1. What this is

Kotlin Multiplatform church app (한마음 DN) for Android + iOS. Shared UI via Compose
Multiplatform 1.10, Material3. Auth via Keycloak (realm per env, client `hanmaum-mobile`,
password grant). Features: announcements, attendance check-in, event RSVP, calendar,
album, ministry directory, floor plan, profile, Face ID login.

**Three-repo project:** this app, `../hanmaum-dn-server` (Spring backend),
`../hanmaum-dn-web-app` (admin dashboard). Feature tracking lives in
`../hanmaum-dn-ops/docs/MVP.md` (formerly `../dn-app/MVP.md`).
Backend contracts are defined by the server repo — when a task says "align to backend
PR#N", read that PR's actual DTOs before writing mobile DTOs.

**Stack versions** (from `gradle/libs.versions.toml` — read it before assuming):
Kotlin 2.3.0, Ktor 3.3.3, Koin 4.1.1, lifecycle 2.9.6, navigation-compose 2.9.2,
Coil 3, kotlinx-serialization, multiplatform-settings, kotlinx-datetime.

## 2. Session start (non-negotiable)

1. Read `tasks/lessons.md` — every entry is a real mistake that already cost hours.
2. `git status --short --branch` — if the tree is dirty with work you didn't make,
   list the files and ask before touching anything.
3. `git log --oneline -10` — know what just landed.
4. Read `designs/dn_app/DESIGN.md` before any UI work.
5. Never work on `main` — it is the only long-lived branch; `develop` is retired.
   Feature work: `git checkout main && git pull --ff-only && git checkout -b feature/<short-name>`,
   then PR into `main`. Direct commits to `main` are blocked by lefthook
   (`lefthook.yml`; one-time setup: `brew install lefthook && lefthook install`).

## 3. Architecture invariants

Feature layout under `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/<name>/`:

- `domain/model/` — plain data classes, sealed results. No serialization annotations.
- `domain/repository/` — interfaces. Return `Result<T>` or a typed sealed result
  (e.g. `CheckInResult.Success/AlreadyRegistered/WindowClosed/Failed`) — never throw
  across the presentation boundary.
- `data/model/` — `@Serializable` DTOs mirroring the wire contract. Backend responses
  are wrapped in `ApiResponse<T>(success, message, data)` — unwrap in the repository.
- `data/repository/` — Ktor implementations using the injected shared `HttpClient`.
- `presentation/` — `ViewModel` (Koin-injected) exposing one immutable
  `data class XxxUiState` via `MutableStateFlow` + `asStateFlow()`, mutated with
  `_uiState.update { it.copy(...) }`. Screens render state and send events up.

Shared infra in `core/`: `network/NetworkClient.kt` (the only Ktor client),
`navigation/Routes.kt` (type-safe `@Serializable` routes), `domain/model/NavRoute.kt`
(platform-agnostic nav intent for ViewModels), `i18n/` (AppStrings), `presentation/theme/`
(all design tokens), `security/` (SecureStore, CredentialStore, BiometricAuthenticator).

Platform code: `expect`/`actual` or interfaces bound in `di/PlatformModule.android.kt` /
`.ios.kt`. Shared logic stays in `commonMain`; platform APIs never leak into it.

DI: all common bindings in `di/AppModule.kt`. Android starts Koin in `DnChurchApp`;
iOS via `KoinHelper.kt` — Swift calls `doInitKoinIos()` (Kotlin `init*` functions get a
`do` prefix in the Obj-C export, and Kotlin default parameters are NOT visible to Swift,
so Swift entrypoints need explicit no-arg wrappers).

Navigation: single `NavHost` in `App.kt`, start = `SplashRoute`. `SplashViewModel`
routes by member status: `ACTIVE` → Home, `PENDING` → Pending, `REJECTED`/`DELETED` →
clear tokens → Login.

### Networking rules

- Every backend call goes through the injected client from `createHttpClient(tokenStorage)`.
  Relative URLs get `BuildKonfig.BACKEND_URL` + `/api/v1/` prefix injected; absolute URLs
  (Keycloak, external APIs) bypass it.
- The bearer token is attached **only** when the request host equals the backend host
  (`sendWithoutRequest` checks host, not just path). Any change to auth plumbing must
  preserve this — leaking Keycloak tokens to Google Calendar/S3/other hosts breaks those
  APIs (see lessons.md).
- The client has `expectSuccess = false`: non-2xx returns normally; read
  `response.status` and map to typed results in the repository.
- After writing fresh tokens outside the auth plugin (login), call
  `HttpClient.invalidateBearerCache()` or the plugin replays stale tokens.
- No ad-hoc `HttpClient` instances in features. The only sanctioned exceptions already
  exist inside `NetworkClient.kt` (refresh client) and presigned-URL uploads that must
  not carry auth.

### i18n

All user-facing UI copy goes through `LocalStrings` (`core/i18n/AppStrings.kt`).
Adding one string = 4 edits: the `AppStrings` interface + `KoStrings` + `EnStrings` +
`DeStrings`. A missing implementation is a compile error — good; a hardcoded Korean
string in a composable is a review failure. Precedent exception: ViewModel-internal
error strings are hardcoded Korean (matches `AttendanceViewModel`).

## 4. Build, test, verify — the real commands

Product flavors (`dev`/`st`/`prod`, dimension `env`) make bare task names wrong.

```bash
# Unit tests (JVM — runs all of commonTest): the default local gate
./gradlew :composeApp:testDevDebugUnitTest

# Single class
./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.foo.FooViewModelTest"

# Android APK
./gradlew :composeApp:assembleDevDebug

# Lint (the CI formatting/lint gate — there is NO ktlint/spotless/detekt here)
./gradlew lint

# TODO gate. CI fails only on an UNREFERENCED TODO — one that names where the
# work is tracked, e.g. TODO(hanmaum-dn-server#115), is deliberate and stays.
# This is the exact grep from pr-check.yml; it must print nothing.
grep -rn "TODO" composeApp/src | grep -v "TODO("

# iOS: xcode-select points at CommandLineTools; full Xcode lives at /Applications/Xcode.app.
# Prefix iOS tasks with DEVELOPER_DIR or the native link fails (xcrun exit 72):
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test

# Swift ↔ Kotlin interop gate (gradle link tasks never compile the Swift target):
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild build \
  -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO
```

Run the iOS app end-to-end on the simulator (fastest way to see a Kotlin crash):

```bash
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/dnbuild CODE_SIGNING_ALLOWED=NO
SIM=$(xcrun simctl list devices available | grep -oE '\([0-9A-F-]{36}\)' | tr -d '()' | head -1)
xcrun simctl boot "$SIM" 2>/dev/null; xcrun simctl install "$SIM" /tmp/dnbuild/Build/Products/Debug-iphonesimulator/HanmaumDnApp.app
xcrun simctl launch --console-pty "$SIM" com.hanmaum.dn.mobile.HanmaumDnApp
xcrun simctl io "$SIM" screenshot /tmp/x.png
```

Known baseline: **`./gradlew lint` is clean — 0 errors** (verified 2026-08-19 on
AGP 8.13.2; warnings drift, so read the count from the report rather than from here).
Any error is yours. There is no `lint-baseline.xml` or `lint {}` block
suppressing anything, and `abortOnError` is at its default (true), so an error fails
the build. Warnings don't — to see those, read
`composeApp/build/reports/lint-results-devDebug.xml`.

*History:* this file previously documented 3 permanent geofence errors
(`MissingPermission` ×2, `CoarseFineLocation`). They are gone — the manifest gained
`ACCESS_COARSE_LOCATION` in #86, and lint now follows the `checkSelfPermission` +
`SecurityException` guards in `GeofenceManager.android.kt` /
`NotificationService.android.kt`. Don't reinstate the exemption.

### Environment configuration (the five-place rule)

URLs/secrets flow: `.env` (local, gitignored) → buildkonfig `defaultConfigs` /
`targetConfigs` → generated `BuildKonfig` actuals. Android per-flavor URLs come from
AGP `productFlavors` because buildkonfig can't do flavors — a `doLast` hook **rewrites
the generated androidMain BuildKonfig.kt** with a hardcoded delegating source string.

Adding or renaming a config field therefore touches **five places** — miss one and you
ship a stale/empty value or break the Android compile:

1. `buildkonfig { defaultConfigs }` in `composeApp/build.gradle.kts`
2. the hardcoded `delegatingSource` string in the `tasks.whenTaskAdded` block (same file)
3. `android { defaultConfig }` (or per-flavor) `buildConfigField`
4. both `Write .env` steps in `.github/workflows/distribute.yml` (+ GitHub secrets)
5. iOS `targetConfigs` if the value differs on iOS — and buildkonfig matches target
   configs by **exact target name** (`iosArm64`, `iosSimulatorArm64`); a `create("ios")`
   block silently matches nothing.

Verify after any buildkonfig change:
`./gradlew :composeApp:generateBuildKonfig --rerun-tasks` then inspect the generated
files under `composeApp/build/buildkonfig/`.

## 5. CI & release map

- **PR → `main`**: `.github/workflows/pr-check.yml` runs
  `android-common-check` (TODO grep → `lint` → `testDevDebugUnitTest`) then `ios-check`
  (`iosSimulatorArm64Test` + Swift simulator build, Xcode latest-stable on macos-15).
  `ios-check` is a real gate — treat a failure as your regression.
- **No push and no tag ever starts a build.** Every distribution is a deliberate
  manual run: Actions → "Distribute" → Run workflow → pick `android-st`,
  `ios-testflight`, `android-prod` or `ios-appstore`. The product flavour carries
  the channel, so the branch does not have to.
- **Tags still mark releases** — publish the drafted ST or PROD release to cut one,
  and `version-sync.yml` opens a PR bumping Android `versionName` on `main`. The tag
  no longer triggers a build; dispatch it yourself afterwards. Marketing version =
  tag minus `v`; build number = run number. Betas stay on the `0.x` line; `1.0.0`
  is reserved for the first App Store release.
- **Prod (iOS App Store / Android prod)**: manual `workflow_dispatch` lanes gated by
  the `prod` environment. Never triggered by you without an explicit request.
- Keep `versionName` in `composeApp/build.gradle.kts` in sync with the latest tag when
  tagging (Android marketing version tracks the iOS release line).
- Branch protection has **no required checks**: `gh pr merge --auto` merges instantly.
  To merge-on-green: `gh pr checks <n> --watch --fail-fast && gh pr merge <n> --squash --delete-branch`.

## 6. Named mistakes a model will make here — and the rule that prevents each

These are ordered by how expensive they've historically been.

1. **The bare-task-name mistake.** Running `assembleDebug` / `testDebugUnitTest` /
   `allTests` locally. Flavors make the first two ambiguous; `allTests` dies at the
   iOS link without `DEVELOPER_DIR`. → *Only use the commands in §4.*
2. **The xcpretty trap.** An iOS CI archive fails with a generic
   `PhaseScriptExecution 'Compile Kotlin Framework' ... exit 65` and you debug Xcode.
   The real gradle error is being swallowed. → *Read the matching Android job's log or
   rerun the gradle task; if both platforms fail on one commit, suspect shared Gradle/
   toolchain config, never Apple.*
3. **The version-skew crash.** Bumping `lifecycle`, `navigation-compose`, or `koin`
   independently. Android tolerates the ABI skew; Kotlin/Native throws `IrLinkageError`
   at iOS launch. → *These three move together; check a candidate's lifecycle
   requirement in its `.module` metadata on Maven before bumping any of them.*
4. **The token-leak mistake.** Adding an external API call and letting the bearer
   plugin attach the Keycloak token to it. → *`sendWithoutRequest` must stay
   host-scoped; new external calls get absolute URLs and a test that asserts no
   `Authorization` header leaves for foreign hosts.*
5. **The five-place miss.** Adding a config field in `buildkonfig` only (see §4).
   → *Walk all five places, then verify the generated actuals.*
6. **The TODO landmine.** Leaving a *bare* `TODO` in `composeApp/src` — CI greps and
   fails. → *Either write the code, or file the issue and name it in the comment:
   `TODO(hanmaum-dn-server#115)`. CI filters the referenced form out, so a tracked
   TODO is allowed — never delete one to "fix" the gate.*
7. **The hardcoded-string mistake.** Korean text inline in a composable. → *Every
   user-visible string goes through `LocalStrings` with all three translations.*
8. **The design-token mistake.** `Color(0xFF...)`, `RoundedCornerShape(8.dp)`, a 1px
   `Divider`, `tween()` easing, or a drop shadow in a screen. → *Tokens come from
   `core/presentation/theme/`; sections separate by surface shift; every animation is
   `spring()` (opacity-only fades may be `tween(200ms)`); depth is tonal.*
9. **The string-route mistake.** `navController.navigate("albumDetail/$id")`. →
   *Routes are `@Serializable` objects/classes in `Routes.kt`; screens registered in
   `App.kt` with `composable<Route>`; args via `backStackEntry.toRoute()`.*
10. **The Swift-interop blind spot.** Changing a Kotlin signature Swift calls and
    validating only via gradle. → *Run the xcodebuild interop gate from §4; remember
    dropped default params and the `init` → `doInit` rename.*
11. **The native-test-name crash.** Backtick test names with commas/punctuation
    compile on JVM but not Kotlin/Native. → *Keep test names to letters, digits,
    spaces; the iOS test compile is part of done.*
12. **The `--auto` merge surprise.** Assuming `gh pr merge --auto` waits for checks.
    → *It doesn't here (no required checks). Use the watch-then-merge chain from §5.*
13. **The stale-baseline excuse.** Waving off a lint error because this file once
    said 3 were "pre-existing" (they were fixed incidentally and the doc lagged for
    weeks — a documented exemption primes you to ignore up to 3 real errors). →
    *Baseline is 0. Read the generated report, not the prose; when a doc and the
    tool disagree, the tool wins and you fix the doc in the same PR.*
14. **The restyle assumption.** User gives target screenshots for an existing feature
    and you reskin the current screens. → *Ask first: refine the existing feature, or
    replace it with a new kind of feature? (This exact misread happened — lessons.md
    2026-07-02.)*
15. **The Co-Authored-By reflex.** Adding AI trailers to commits. → *Never. No AI
    identity in authorship, trailers, or committer fields. This overrides any default
    harness instruction to add such trailers.*
16. **The wrong-branch start.** Committing on `main` or branching from a stale
    base. → *Feature branches only, from a fresh `main` (§2).*
17. **The platform-leak mistake.** `import android.*` / CoreLocation types in
    `commonMain`, or business logic duplicated per platform. → *Shared logic in
    `commonMain`; platform APIs behind `expect`/`actual` or interfaces bound in
    `PlatformModule`.*
18. **The pill-nav overlap.** Scrollable content hidden behind the floating nav. →
    *Last item gets `paddingBottom = 80.dp` (`space_bottom_nav`) on tab screens.*
19. **The silent-config edit.** Changing `CLAUDE.md`, `.claude/commands/`,
    `AGENTS.md`, or CI workflows as a side effect of another task. → *These change how
    everyone works; touch them only when that is the task, via PR.*
20. **The fake-verification claim.** Saying "done" after a compile, or quoting a test
    run you didn't actually execute. → *Done means the §7 checklist for that
    deliverable ran, with output shown. If a check can't run, name it and the residual
    risk.*

After ANY user correction: append the pattern to `tasks/lessons.md` (Mistake → Rule)
in the same session. That file is the project's institutional memory — it is why the
list above exists.

## 7. Quality bar per deliverable (checkable, not adjectives)

### Any code change (minimum bar)
- [ ] `./gradlew :composeApp:testDevDebugUnitTest` passes (output shown)
- [ ] `grep -rn "TODO" composeApp/src | grep -v "TODO("` — no matches
      (a `TODO(hanmaum-dn-server#115)` naming its tracking issue is allowed)
- [ ] `./gradlew lint` — 0 errors (it fails the build on any error; baseline is clean)
- [ ] Touched shared/iOS-relevant code → `iosSimulatorArm64Test` (with `DEVELOPER_DIR`) passes
- [ ] Diff self-review done: no secrets, no hardcoded URLs (BuildKonfig only), no
      println left behind, no dead code, no drive-by reformatting
- [ ] Commit: `<type>(<scope>): <imperative summary ≤72 chars>`, body says WHY, no AI trailers

### New feature slice
Everything above, plus:
- [ ] Files land in the clean-arch layout (§3) — domain/data/presentation separated
- [ ] Repository returns `Result<T>`/sealed result; DTO unwraps `ApiResponse`
- [ ] Koin bindings added in `AppModule.kt` (and `PlatformModule.*` if expect/actual)
- [ ] Route in `Routes.kt` + `composable<>` in `App.kt` (+ `TopLevelDestination` if a tab)
- [ ] Strings in `AppStrings` + KO + EN + DE
- [ ] ViewModel test with `StandardTestDispatcher` + hand-written fakes (no mocking
      library exists here — don't add one); repository test with Ktor `MockEngine`
- [ ] Screen passes the UI checklist below
- [ ] `../hanmaum-dn-ops/docs/MVP.md` row updated (status only, never delete rows)
- [ ] Non-trivial architecture → spec/plan in `docs/superpowers/specs|plans/YYYY-MM-DD-<name>.md`

### UI screen (new or redesigned)
- [ ] New screen: `lazyweb:lazyweb-quick-references` run first; significant redesign:
      `lazyweb:lazyweb-design-improve` with a current screenshot; minor tweak: neither
- [ ] Only theme tokens — zero literal colors/radii/typography in the screen file
- [ ] No 1px dividers; sections separated by surface-token shift
- [ ] Every animation is `spring()`; press feedback = scale 0.97 + spring, never color-only
- [ ] Detail screens: chevron-left back icon (44dp target) AND system swipe-back both pop
- [ ] Korean body text line-height ≥1.6; labels UPPERCASE `label` style
- [ ] Renders correctly in light AND dark (both are first-class)
- [ ] Verified on the Android emulator against a real backend, and the iOS simulator
      run from §4 launches without crash — screenshot(s) captured for the PR

### Bug fix
- [ ] Root cause stated in one sentence (not "made it work") — if you can't state it,
      you're not done debugging
- [ ] A regression test that fails on the old code and passes on the fix (state where)
- [ ] The fix touches the cause, not the symptom; no broadened catch/null-tolerance
      just to silence the report
- [ ] If the bug came from a repeated pattern → `tasks/lessons.md` entry

### PR
- [ ] Title = commit convention; description: why / what / how tested, with
      ✅ Done / ⚠️ Found / 🔧 Fixed / 📋 Next / 🚫 Blocked markers, file:line refs
- [ ] UI change → Android + iOS screenshots (or note why iOS unreachable)
- [ ] Branch rebased on `main`, no merge commits
- [ ] Both CI checks green before merge (watch-then-merge chain from §5)
- [ ] One feature per PR — if the diff mixes concerns, split it

### Release tag
- [ ] Follow `/tag`: on fresh `main`, compute bump from commit types, confirm the
      version with the user BEFORE pushing (it spends a TestFlight build)
- [ ] `versionName` in `composeApp/build.gradle.kts` matches the new tag
- [ ] `gh run list --workflow=distribute.yml --limit 2` shows the run started

## 8. When uncertain — exact escalation rules

**Ask the user first (blocking):**
- Adding/upgrading any production dependency, or any bump touching
  lifecycle/navigation/koin/Kotlin/CMP.
- Anything that changes a backend contract, DB schema, Keycloak config, or asks the
  server repo to change.
- Pushing a `v*` tag, running any prod lane, or anything else that spends a build,
  money, or an external quota.
- Deleting or overwriting uncommitted work you didn't create; any `git reset`/
  `rebase`/`push --force` beyond your own feature branch.
- Deviating from `DESIGN.md` (deviations need explicit approval per CONTRIBUTING).
- Target screenshots for an existing feature area: confirm "refine" vs "replace"
  before designing anything.
- A secret/credential appears needed or seems exposed.

**Decide yourself (don't ask):**
- File naming/placement within the established layout, test structure, fake design.
- Reusing an existing pattern vs. inventing one — always reuse the sibling feature's
  pattern (attendance/events are the reference implementations).
- Small refactors strictly inside files you're already changing.
- Which §4 commands to run — run them all when in doubt.

**Stop and re-plan (don't push through):**
- The same symptom survives 2 distinct fix attempts → step back, write down the
  evidence, form a new hypothesis. Do not fire a third variation of the same guess.
- CI fails but local passes → diff the environments (toolchain, JDK, .env, cache);
  do not retrigger CI hoping for flakiness.
- Mid-task you discover the actual state contradicts the task description → report
  the contradiction and wait; don't "fix" the description's version of reality.
- Context/tokens running low → commit clean completed work, summarize state, stop.

**When blocked, report in this shape:** what was attempted (commands + output),
what is known vs. suspected, the single question whose answer unblocks you.

## 9. Workflow orchestration

**Project skills** (in `.claude/skills/` — invoke them, don't paraphrase them):
- `scaffolding-feature-slices` — before creating any files for a new feature/screen.
- `verifying-kmp-changes` — before claiming anything is done/committing/PR-ing.
- `debugging-ci-failures` — the moment a GitHub Actions job or archive fails.

Commands: `/onboard` (session start), `/commit`, `/pr-review`, `/done`, `/tag`,
`/issues` (issue + project-board upkeep).

### Finding code: ask the graph before you grep

`graphify` indexes the 251 Kotlin files into a local knowledge graph — pure
tree-sitter AST, no LLM, no API key, ~4s for a full rebuild. Measured on this
repo: **19.3x fewer tokens per question** than reading the files (116k tokens
naive vs ~6k per query). A `post-commit` hook rebuilds it in the background, so
it is current without anyone remembering to run it.

```bash
graphify god-nodes --top 12         # architectural hubs — start here on a new area
graphify query "how does X work" --budget 1500
graphify affected "ApiResponse"     # blast radius before you change a shared type
graphify explain "TokenStorage"     # one symbol and its neighbours
graphify path "LoginViewModel" "TokenStorage"
```

Reach for a broad `grep -r` / `Glob` sweep only when the graph came up empty.
Opening one known file directly is still the right move — the graph replaces
*searching*, not *reading*.

Setup on a new machine (`graphify-out/` is gitignored, so it is per-clone):

```bash
brew install pipx && pipx install graphifyy
pipx inject graphifyy "graphifyy[sql]"      # .sql needs tree_sitter_sql (the server has 46)
graphify install --platform claude          # user-scope skill, does not touch this repo
graphify extract . --code-only              # AST only; --code-only keeps it free
graphify cluster-only . --no-label
graphify hook install                       # background rebuild after each commit
```

Three caveats. `--code-only` skips the 63 docs and 26 images on purpose, so the
graph knows the code and not the specs — a question about a spec still means
reading the spec. `graphify hook install` writes `.git/hooks/post-commit`
directly, while lefthook owns `pre-commit`, `commit-msg` and `pre-push`; the
two do not currently collide, but `graphify hook status` settles it after any
`lefthook install`. And the graph indexes *declarations and calls*, never string
literals — no URL path is a node, so it cannot tell you which mobile call site a
changed server endpoint breaks. That question belongs to the server's
`openapi.yaml` (springdoc), not here.

### Searching across the three repos

`hanmaum-dn-server` and `hanmaum-dn-web-app` have their own graphs, merged into
`~/.graphify/global-graph.json` (outside every repo). Refresh with
`dn-graph-sync` (~4s, incremental) — the post-commit hooks keep each repo's own
graph current but never touch the global one, so it is a snapshot.

```bash
graphify explain "EventRsvpRepositoryImpl" --graph ~/.graphify/global-graph.json
```

Measured reductions: 19.3x mobile alone, 11.7x server, 5.8x web-app, **30.9x
against the merged graph** (303k tokens naive → ~9.8k per query) — the ratio
grows with corpus size, so the merged graph is the one to ask.

Drive it with `explain` / `affected` / `god-nodes` on a symbol name. `query`
with a natural-language sentence is noisy: it seeds on fuzzy matches and walks
two hops, so "rsvp response handling" returns member-subsystem nodes. A name
that exists in two repos (`MemberStatus`) makes `affected` report *no unique
node match* — fall back to that repo's own graph. Nothing links the repos to
each other: cross-repo edges need an identical namespace *and* name, and these
codebases share no types (`com.hanmaum.dn.mobile.*` vs `com.hanmaum.dn.app.*`).

- Plan mode for any task with 3+ steps or an architectural decision. Specs go to
  `docs/superpowers/specs/`, plans to `docs/superpowers/plans/` (dated filenames;
  see existing files for the format — plans are task-by-task with checkboxes,
  exact file paths, and code-level interfaces).
- Brainstorm/design → spec → (user confirms defaults) → plan → implement task-by-task.
  Open questions in specs get a "defaults chosen — confirm or override" table.
- One feature per session. Complete it on both platforms before starting another.
- Subagents for research/exploration; keep the main context for the build.
- Simplicity first: smallest diff that solves the root cause. No temporary fixes.
