# Release Automation — Runbook

Automated, label-driven GitHub Releases with generated notes, on two tracks:
**staging** (`develop`) and **production** (`main`). Built on
[Release Drafter](https://github.com/release-drafter/release-drafter).

## TL;DR

- Open/merge PRs as usual. Your Conventional-Commit **PR title** auto-applies a
  bump label; a manual `major` / `minor` / `patch` label overrides it.
- Every merge to `develop` updates a **draft** `ST-Release vX.Y.Z-st`.
  Every merge to `main` updates a **draft** `PROD-Release vX.Y.Z`.
- **Nothing is tagged or built** until you open the draft in the **Releases**
  tab and click **Publish**. Publishing is the deliberate, human release act.
- Publishing an **ST** draft → tag `vX.Y.Z-st` → **iOS TestFlight** (staging).
- Publishing a **PROD** draft → tag `vX.Y.Z` → release marker only. Shipping to
  the App Store stays the manual `Distribute → ios-appstore` lane.
- On publish, Android `versionName` in `composeApp/build.gradle.kts` is
  auto-synced to the numeric version by a bot commit `[skip ci]`.
- Each note line credits the PR author by GitHub login (`- <title> (#N) @author`).
  Automation/AI logins are excluded (`exclude-contributors`), and since notes
  only list merged PRs authored by humans, an AI name can never appear.

## How the version is decided (labels)

The next version is resolved from PR labels since the last release on that
track. The highest bump across all included PRs wins; unlabeled PRs default to
**patch**.

| PR title (Conventional Commit) | Auto label | Bump (pre-1.0) |
|---|---|---|
| `feat(scope): …` | `feature` | minor |
| `fix(scope): …` / `perf: …` | `fix` | patch |
| `feat!: …` or body has `BREAKING CHANGE` | `breaking` | **minor** (capped, see below) |
| `chore/docs/style/refactor/test/ci/build: …` | `chore` | patch |
| anything else, no label | — | patch |

Manual override: add `major`, `minor`, or `patch` directly to a PR.

### The 1.0.0 guard (pre-1.0 "zerover")

We are on the `0.x` beta line and **`1.0.0` is reserved for the first App Store
launch**. So every automatic bump — including breaking changes — is
intentionally **capped at `minor`**; nothing the automation does can jump to
`1.0.0`. To release `1.0.0`, publish a `v1.0.0` draft by hand at launch time.

**After 1.0.0 ships:** in both `.github/release-drafter-*.yml`, move the
`major` / `breaking` labels out of the `minor:` group into a real `major:` group
to restore standard SemVer.

## The two tracks

| | Staging | Production |
|---|---|---|
| Branch | `develop` | `main` |
| Draft title | `ST-Release vX.Y.Z-st` | `PROD-Release vX.Y.Z` |
| Tag on publish | `vX.Y.Z-st` (pre-release) | `vX.Y.Z` |
| Config | `release-drafter-st.yml` | `release-drafter-prod.yml` |
| Publish triggers | iOS TestFlight (staging backend) | release marker; App Store = manual |

The two version lines are computed **independently** (`filter-by-commitish`), so
the `-st` line races ahead as features land on `develop`, while the prod line's
number is computed from what has been merged into `main`. Promote deliberately:
the prod version is not automatically "the latest `-st` minus the suffix".

## Publishing a release

1. Go to the repo **Releases** tab. The relevant draft is at the top with the
   computed version and categorized notes.
2. Review the notes and the version. Edit the notes if you like.
3. Click **Publish release**. This creates the tag and (for `-st`) kicks off the
   TestFlight build. A tag push spends a real ~15-min TestFlight build — publish
   when you mean it.
4. The `version-sync` workflow commits the matching `versionName` back to the
   release's branch (`[skip ci]`, no re-build).

## Shipping production to the App Store

Publishing the `PROD-Release` draft only cuts the `vX.Y.Z` tag — it does **not**
submit to the App Store (CLAUDE.md prod-lane invariant). When you mean to ship:
**Actions → Distribute → Run workflow → `ios-appstore`** (gated by the `prod`
environment approval). The build derives its marketing version from the latest
`v*` tag via `git describe`.

## Files

- `.github/workflows/release-drafter.yml` — runs the drafter (autolabel on PRs,
  draft on merge), routed to the right track by branch.
- `.github/workflows/version-sync.yml` — syncs `versionName` on publish.
- `.github/release-drafter-st.yml` / `-prod.yml` — per-track config.
- `.github/workflows/distribute.yml` — TestFlight fires on `v*-st`; App Store is
  the manual `ios-appstore` lane.

## Caveats / things to watch

- **Bot push vs branch protection.** `version-sync` pushes the `versionName`
  commit directly to `develop`/`main` with the built-in `GITHUB_TOKEN`. If
  branch protection is later set to *require pull requests* on those branches,
  that push will be rejected — switch the step to open a version-bump PR then.
- **Label required to exist.** The bump/category labels are pre-created in the
  repo. If you rename one in a config, create the matching GitHub label too.
- **First run.** Before any track has a release, the drafter starts the line
  from `0.0.0` + the first bump (e.g. a `feat` → `0.1.0`). Seed the starting
  point by publishing the first draft with the version you actually want
  (we are currently at `v0.5.0`).
