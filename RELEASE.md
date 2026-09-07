# Release Automation — Runbook

Automated, label-driven GitHub Releases with generated notes, on two tracks:
**staging** and **production**. Both draft off `main` — `develop` is retired and
the branch no longer exists. Built on
[Release Drafter](https://github.com/release-drafter/release-drafter).

## TL;DR

- Open/merge PRs as usual. Your Conventional-Commit **PR title** auto-applies a
  bump label; a manual `major` / `minor` / `patch` label overrides it.
- Every merge to `main` updates **both** drafts: `ST-Release vX.Y.Z-st` and
  `PROD-Release vX.Y.Z`.
- **Nothing is tagged** until you open the draft in the **Releases** tab and
  click **Publish**. Publishing is the deliberate, human release act.
- **Publishing never builds anything.** Both tracks only cut a tag and record
  the note. Every build is a separate manual `Actions → Distribute` run.
- **Publish before you dispatch — every time, on both tracks.** See
  [One release, one note](#one-release-one-note) below; this is the rule the
  rest of the runbook exists to serve.
- On publish, `version-sync` opens a PR bumping Android `versionName` in
  `composeApp/build.gradle.kts` to the numeric version.
- Each note line credits the PR author by GitHub login (`- <title> (#N) @author`).
  Automation/AI logins are excluded (`exclude-contributors`), and since notes
  only list merged PRs authored by humans, an AI name can never appear.

## One release, one note

**Every distribution gets a published release note first — staging and
production alike.** Publish the draft, *then* dispatch the build.

Publishing costs nothing. `distribute.yml` runs on `workflow_dispatch` only, so
creating the tag starts no build and spends no runner minutes. There is no
situation in which skipping it saves anything.

The order matters because the build reads its version from the tag:

```
Publish draft  →  tag vX.Y.Z-st now exists
               →  Actions → Distribute → ios-testflight
               →  git describe finds the new tag
               →  TestFlight shows X.Y.Z (<run number>)
```

Dispatch a build without publishing first and it silently inherits the previous
tag. That is not hypothetical. Builds **76, 77, 78 and 79** all reached
TestFlight as marketing version **0.7.1** with materially different content — 76
had none of the registration fixes, 79 had all of them — telling them apart
required reading the build number. Twenty-three merged PRs went to testers with
no note saying what had changed.

If you find yourself about to dispatch a build and the top draft is empty, that
is the signal that the last publish already covered `main` — not a reason to
skip the step.

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
| Branch | `main` | `main` |
| Draft title | `ST-Release vX.Y.Z-st` | `PROD-Release vX.Y.Z` |
| Tag on publish | `vX.Y.Z-st` (pre-release) | `vX.Y.Z` |
| Config | `release-drafter-st.yml` | `release-drafter-prod.yml` |
| On publish | tag + note only | tag + note only |
| How it ships | `Distribute → ios-testflight` / `android-st` | `Distribute → ios-appstore` / `android-prod` |

Both lines draft off the same branch and are kept apart by **tag shape** alone.
They share one ascending version base: `release-drafter-prod.yml` sets
`include-pre-releases: true`, so the prod line counts the `-st` releases too.

It used to be left at the default (false), on the idea that the prod line
deserved a base of its own. What that produced was a prod draft numbered
**below** what was already in TestFlight — `v0.6.0` while `v0.7.1-st` was live —
regenerated on every push to `main`. A production release must never be numbered
under what has shipped.

The staging config carries no such key, and must not: its action step passes
`prerelease: true`, and `include-pre-releases` has no effect in that case.

> The staging config pointed at `refs/heads/develop` long after that branch was
> deleted. Release Drafter found no commits there, left the draft untouched from
> July onward, and still reported success on every run — nothing to add is not an
> error. Twenty-three merged PRs went unrecorded. If a draft stops growing, check
> `commitish` before anything else.

## Publishing a release

1. Go to the repo **Releases** tab. The relevant draft is at the top with the
   computed version and categorized notes.
2. Review the notes and the version. Edit the notes if you like.
3. Click **Publish release**. This creates the tag and publishes the note. It
   starts **no build** — no tag and no push triggers `distribute.yml`.
4. `version-sync` opens a PR bumping `versionName` to match. Merge it.
5. Only now: **Actions → Distribute → Run workflow**, pick the lane. The build
   picks up the tag you just created via `git describe`.

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
- `.github/workflows/distribute.yml` — every lane is a manual
  `workflow_dispatch`; no push and no tag starts a build.

## Caveats / things to watch

- **`version-sync` needs the PR permission.** It pushes a bot branch and opens a
  PR (`main` requires pull requests, so it cannot commit directly). This fails
  with `GitHub Actions is not permitted to create or approve pull requests`
  unless **Settings → Actions → General → Workflow permissions → "Allow GitHub
  Actions to create and approve pull requests"** is on. Its earlier runs looked
  green only because the no-downgrade guard had skipped them.
- **Label required to exist.** The bump/category labels are pre-created in the
  repo. If you rename one in a config, create the matching GitHub label too.
- **First run.** Before any track has a release, the drafter starts the line
  from `0.0.0` + the first bump (e.g. a `feat` → `0.1.0`). Seed the starting
  point by publishing the first draft with the version you actually want.
- **The prod track has never shipped.** Across every `distribute.yml` run to
  date only `android-st` and `ios-testflight` have executed; `android-prod` and
  `ios-appstore` never have. `1.0.0` at App Store launch will be the first real
  production release.
