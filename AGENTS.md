# AGENTS.md — hanmaum-dn-mobile-app

Repository-level instructions for Codex, and the only Codex file that is committed. Treat it as the durable source of truth for how to work in this repo, and keep it self-contained: everything Codex must know to verify a change is written out here, not delegated to a file that may not exist on this machine. Shared long-form workflows live under `docs/codex/`.

## Project Snapshot

- Kotlin Multiplatform mobile app for Android and iOS.
- Shared UI: Compose Multiplatform 1.10.0 + Material3.
- Architecture: feature-first clean architecture with shared KMP domain/data/presentation layers.
- Auth: Keycloak; tokens stored through `TokenStorage` using multiplatform-settings.
- Networking: Ktor 3.3.3 through `core/network/NetworkClient.kt` only.
- DI: Koin 4.1.1 via `di/AppModule.kt`, `DnChurchApp`, and `KoinInit.kt`.
- Version skew warning: `lifecycle`, `navigation-compose`, and `koin` are ABI-coupled. Bumping one alone compiles on Android but throws `IrLinkageError` at iOS launch. Move them together.
- Navigation: type-safe `@Serializable` route objects in `core/navigation/Routes.kt`.

## Start-of-Session Protocol

1. Read `tasks/lessons.md` before changing anything.
2. Check `git status --short --branch`; if there are user changes, list them and do not overwrite them.
3. Inspect related code before planning or editing.
4. For any task with 3+ steps, architectural choices, or uncertain scope, use a written plan first.
5. If work goes sideways, stop, re-plan, then continue with the smallest safe correction.

## Senior Mobile Engineering Defaults

- Solve the root cause, not the symptom.
- Prefer small, reviewable changes that preserve existing style.
- Shared business logic belongs in `commonMain`; platform APIs stay in platform source sets or behind `expect`/`actual`/interfaces.
- UI must be state-driven: ViewModels expose immutable UI state via `StateFlow`; composables render state and send events back.
- Repositories are the boundary to data sources; ViewModels/composables must not call Ktor, Firebase, Settings, GPS, or platform APIs directly.
- Dependencies are not added casually. Ask before new production dependencies or risky upgrades.
- Do not introduce database/schema/server contract changes unless explicitly requested.
- Security/privacy: never commit secrets, tokens, API keys, keystores, or real user data. Keep environment URLs in BuildKonfig or environment config.

## Architecture Rules

Feature layout under `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/<feature>/`:

- `domain/model/` — plain shared models.
- `domain/repository/` — repository interfaces.
- `data/model/` — DTOs and serialization types.
- `data/repository/` — Ktor/settings/cache-backed implementations.
- `presentation/` — ViewModels, UI state, Compose screens/components.

Shared infrastructure lives in `core/`:

- `core/network/NetworkClient.kt` owns Ktor client configuration, base URL handling, JSON, logging, and bearer auth.
- `core/domain/repository/TokenStorage.kt` and `core/data/repository/TokenStorageImpl.kt` own token persistence.
- `core/navigation/Routes.kt` owns type-safe navigation route objects.
- `core/domain/model/NavRoute.kt` is the platform-agnostic navigation intent enum for ViewModels.
- `core/presentation/theme/` owns design-system tokens; do not hardcode one-off colors/shapes/typography in screens.

## Networking Rules

- All backend calls go through the injected shared `HttpClient` from `createHttpClient(tokenStorage)`.
- Do not create ad-hoc Ktor clients in features unless a feature explicitly needs an isolated third-party client and the auth-leak risk is reviewed.
- Bearer tokens must only be sent to the Hanmaum backend host. Never leak Keycloak tokens to pCloud, Google Calendar, or other external hosts.
- Login/Keycloak calls use absolute URLs and bypass backend base-path injection.
- Feature repositories return `Result<T>` or existing project error patterns; avoid throwing across presentation boundaries.

## UI, Design, and Accessibility

All screens must conform to `designs/dn_app/DESIGN.md`.

- New screen from scratch: run `lazyweb:lazyweb-quick-references` first when the lazyweb tool is available; otherwise state it is unavailable and continue with `DESIGN.md`.
- Significant redesign: run `lazyweb:lazyweb-design-improve` with a current screenshot when available; otherwise state the fallback.
- Minor modifications: follow `DESIGN.md` tokens directly.
- Animations: use spring-based specs with `animateFloatAsState` / `AnimatedVisibility`; never linear or ease-in-out transitions.
- Compose performance: avoid expensive work in composables; compute in ViewModels or `remember`; use stable lazy-list keys; use `derivedStateOf` for rapidly changing derived UI state.
- Accessibility: support readable labels/content descriptions, sufficient contrast, scalable text, and tap targets appropriate for mobile.

## Build, Test, and Verification Commands

Product flavors (`dev`/`st`/`prod`, dimension `env`) make bare Gradle task names
ambiguous, and every iOS task needs `DEVELOPER_DIR` because `xcode-select` points at
CommandLineTools. Use exactly these:

```bash
# Unit tests (JVM — runs all of commonTest): the default local gate
./gradlew :composeApp:testDevDebugUnitTest

# Single test class
./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.YourTestClass"

# Android debug APK
./gradlew :composeApp:assembleDevDebug

# Lint — the CI formatting/lint gate. There is NO ktlint/spotless/detekt here.
# Baseline is 0 errors; any error fails the build and is yours. Read the count
# from the report, never from a doc:
#   grep -c 'severity="Error"' composeApp/build/reports/lint-results-devDebug.xml
./gradlew lint

# TODO gate — CI greps and fails the build on any match
grep -rn "TODO" composeApp/src   # must print nothing

# iOS native tests — without DEVELOPER_DIR this dies with xcrun exit 72
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  ./gradlew :composeApp:iosSimulatorArm64Test

# Swift interop gate — Gradle link tasks never compile the Swift target, so
# Kotlin signature changes Swift calls break only in the post-merge archive.
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild build \
  -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO

# Clean build
./gradlew clean
```

Never use `allTests`, `assembleDebug`, `testDebugUnitTest`, or
`compileDebugKotlinAndroid`: the first fails at the iOS native link on a machine
without full Xcode, and the rest are flavor-ambiguous.

For iOS runtime verification, run `iosApp/iosApp.xcodeproj` on a simulator — either
from Xcode, or headless via `xcodebuild build` + `xcrun simctl install/launch`, which
prints Kotlin crash stack traces directly to the console.

## Definition of Done

Before calling work complete:

1. Add/update tests for behavior changes.
2. Run the smallest relevant tests first, then `./gradlew :composeApp:testDevDebugUnitTest`.
3. `grep -rn "TODO" composeApp/src` — must print nothing (CI fails the build on any match).
4. `./gradlew lint` — 0 errors.
5. For shared/`commonMain`/iOS-reachable code, run the `iosSimulatorArm64Test` command above with its `DEVELOPER_DIR` prefix. If a Kotlin declaration Swift calls changed, also run the Swift interop gate.
6. For Android deliverables, run `./gradlew :composeApp:assembleDevDebug` or explain why not.
7. Review the diff for regressions, secrets, auth leakage, hardcoded URLs, TODO/FIXME, dead code, and architecture drift.
8. Summarize changed files, verification results, and residual risk.

A compile is not a verification, and a claim of Done without the commands' output is a false claim.

If a check cannot be run, say exactly why and what risk remains.

## Codex Workflow Assets

**Shared** (committed, present in every checkout):

- Playbook: `docs/codex/mobile-engineering-playbook.md`
- Review checklist: `docs/codex/code-review.md`

**Local** (per-machine, git-ignored — may not exist):

`.codex/commands/*.md` and `.agents/skills/*/SKILL.md` are each developer's own
prompt templates and skills. Use them when they are present, but never assume
they are: they are not part of the repo, they are not reviewed, and they can
drift from this file without anyone noticing. When they disagree with AGENTS.md,
**AGENTS.md wins** — in particular for the verification commands above, which are
the ones CI actually enforces.

## OpenAI/Codex Documentation

Always use the OpenAI developer documentation MCP server if work involves OpenAI API, ChatGPT Apps SDK, Codex, Codex configuration, skills, plugins, or MCP. Prefer official OpenAI docs over memory or third-party summaries.

## Git Rules

- Never work on `main` or `develop`. `develop` is the integration branch; branch `feature/<short-name>` from a fresh `develop` and PR back into it. Direct commits to `main`/`develop`/`dev` are blocked by lefthook (`lefthook.yml`).
- Never add `Co-Authored-By:` trailers to commits, or any other AI/tool identity in authorship, trailers, or committer fields.
- Do not rebase, reset, discard, or overwrite user work without explicit permission.
- Commit messages: `<type>(<scope>): <imperative summary max 72 chars>`.
- Types: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`, `perf`, `revert`.
