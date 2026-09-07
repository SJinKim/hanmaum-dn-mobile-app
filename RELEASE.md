# Release Automation — Runbook

Label-driven GitHub Releases with generated notes, on two tracks: **staging**
and **production**. Both draft off `main` — `develop` is retired and the branch
no longer exists. Built on
[Release Drafter](https://github.com/release-drafter/release-drafter).

Everything here is checked against the workflows in `.github/`. When this file
and a workflow disagree, **the workflow wins** and this file gets fixed in the
same PR — that is CLAUDE.md rule 13, and this runbook has been on the losing
side of it before (see [Troubleshooting](#troubleshooting)).

## The loop

```
merge a PR   →  autolabeler labels it from the PR title
             →  both drafts refresh (nothing tagged, nothing built)

ready to ship?
   1. Publish the draft      → creates the tag + the note.  No build.
   2. Merge the version-sync PR
   3. Actions → Distribute   → the build reads the new tag
```

Three separate acts. Steps 1 and 3 are both deliberate and both manual; the
automation never joins them for you.

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

If you are about to dispatch a build and the top draft is empty, that is the
signal that the last publish already covered `main` — not a reason to skip the
step.

## Publishing a release

**Before you publish**

- [ ] The draft's version is above everything already shipped —
      `git describe --tags --abbrev=0 origin/main` shows what a build would use
      *right now*, and the new tag must sort above it.
- [ ] The notes read as a changelog, not a commit dump. Edit them; the drafter's
      output is a starting point, not the deliverable.
- [ ] Nothing sits in the uncategorised block above `## 🚀 Features` — an entry
      there means its PR carries no category label (see
      [Labels](#how-the-version-is-decided-labels)).

**Publish**

1. **Releases** tab → the relevant draft is at the top with its computed version
   and categorised notes.
2. Review and edit. This is the last cheap moment to fix wording.
3. **Publish release.** This creates the tag and publishes the note. It starts
   **no build** — no tag and no push triggers `distribute.yml`.
4. `version-sync` opens a PR bumping Android `versionName` to match. **Merge
   it.** No status checks are required on `main`, so `gh pr merge <n> --squash`
   goes through immediately; still let `pr-check` finish if you can.
5. **Actions → Distribute → Run workflow**, pick the lane. The build resolves
   its marketing version from the tag you just created.

**After the build**

- [ ] `git describe --tags --abbrev=0 origin/main` equals the version you meant
      to ship.
- [ ] The store/TestFlight entry shows that version, with the run number as the
      build number.

## How the version is decided (labels)

The next version is resolved from the labels on PRs merged since the last
release **on that track**. The highest bump wins; unlabeled PRs default to
**patch**. The autolabeler applies the label from your Conventional-Commit PR
title, so normal commit discipline drives the bump; a manual `major` / `minor` /
`patch` label on a PR overrides it.

| PR title (Conventional Commit) | Auto label | Note category | Bump (pre-1.0) |
|---|---|---|---|
| `feat!: …` / body has `BREAKING CHANGE` | `breaking` | 💥 Breaking Changes | **minor** (capped, see below) |
| `feat(scope): …` | `feature` | 🚀 Features | minor |
| `fix(scope): …` / `perf: …` | `fix` | 🐛 Fixes | patch |
| `chore/docs/style/refactor/test/ci/build: …` | `chore` | 🧹 Maintenance | patch |
| anything else, no label | — | *(uncategorised, top of the note)* | patch |

Note that `feat!:` gets `breaking` **only** — the `feature` pattern requires a
`:` straight after the scope, so the `!` form does not match it. That is why
`breaking` needs a category of its own; without one, the single most important
kind of change lands in the uncategorised block.

The configs also list `enhancement`, `bug`, `documentation`, `refactor`,
`dependencies`, `ci` and `perf` as accepted labels. Only the first three exist
in the repo — `refactor`, `dependencies`, `ci` and `perf` do **not**, so nothing
can ever carry them. They are harmless (an unmatched label matches nothing) but
do not read them as available; the autolabeler folds all of those PR types into
`chore` anyway. Create the GitHub label first if you want to apply one by hand.

### The 1.0.0 guard (pre-1.0 "zerover")

We are on the `0.x` beta line and **`1.0.0` is reserved for the first App Store
launch**. Every automatic bump — including breaking changes — is intentionally
**capped at `minor`**; nothing the automation does can jump to `1.0.0`. To
release `1.0.0`, publish a `v1.0.0` draft by hand at launch time.

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

**The prod track has never run.** Across every `distribute.yml` execution to
date, only `android-st` and `ios-testflight` have executed. Until the App Store
launch the `-st` line *is* the release line, and the "Latest" badge sits on an
old tag because GitHub never marks a pre-release as latest. That is cosmetic —
do not "fix" it by re-tagging.

## Shipping production to the App Store

Publishing the `PROD-Release` draft only cuts the `vX.Y.Z` tag — it does **not**
submit to the App Store (CLAUDE.md prod-lane invariant). When you mean to ship:
**Actions → Distribute → Run workflow → `ios-appstore`** (gated by the `prod`
environment approval). The build derives its marketing version from the latest
`v*` tag via `git describe`.

## Troubleshooting

Every row below actually happened in this repo.

| Symptom | Cause | Fix |
|---|---|---|
| Drafter is green on every run, draft never grows | `commitish` points at a branch that no longer exists — finding nothing to add is not an error | Check `commitish` in the config **first**; it must be `refs/heads/main` |
| TestFlight shows the same version for builds with different content | A build was dispatched without publishing a draft; `git describe` returned the old tag | Publish first — see [One release, one note](#one-release-one-note) |
| Prod draft keeps reappearing with a version below what shipped | `include-pre-releases` was false, so the base ignored every `-st` release | Already fixed; if it returns, check that key in `release-drafter-prod.yml` |
| `version-sync` fails: *GitHub Actions is not permitted to create or approve pull requests* | Repo setting is off | **Settings → Actions → General → Workflow permissions** → enable "Allow GitHub Actions to create and approve pull requests" |
| `version-sync` is green but nothing changed | Its no-downgrade guard skipped the run — the tag's version is not above the current `versionName` | Expected; only a real bump opens a PR |
| An entry sits above `## 🚀 Features` with no category | Its PR has no label in any category | Label the PR and re-run the drafter (`Actions → Release Drafter → Run workflow`) |

## Files

- `.github/workflows/release-drafter.yml` — autolabels PRs, refreshes both
  drafts on every push to `main`, and can be dispatched by hand to refresh.
- `.github/workflows/version-sync.yml` — on publish, opens a PR aligning Android
  `versionName` with the tag. Has a no-downgrade guard.
- `.github/release-drafter-st.yml` / `-prod.yml` — per-track config.
- `.github/workflows/distribute.yml` — every lane is a manual
  `workflow_dispatch`; no push and no tag starts a build.

## Caveats / things to watch

- **`version-sync` needs the PR permission.** It pushes a bot branch and opens a
  PR — `main` carries a ruleset requiring pull requests, so it cannot commit
  directly. See the Troubleshooting row above.
- **Labels must exist before you apply one.** Four names in the configs
  (`refactor`, `dependencies`, `ci`, `perf`) have no GitHub label behind them.
- **No status checks are required on `main`.** The ruleset requires a PR and
  zero approvals, so `gh pr merge --auto` merges immediately rather than
  waiting. To merge on green:
  `gh pr checks <n> --watch --fail-fast && gh pr merge <n> --squash --delete-branch`.
- **First run on a track.** With no release yet, the drafter starts from `0.0.0`
  plus the first bump. Seed the line by publishing the first draft with the
  version you actually want.
