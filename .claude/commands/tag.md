# /tag — Publish a release, then build it (→ TestFlight)

A release here is **three separate acts**. Nothing joins them automatically, and no tag
or push ever starts a build. `RELEASE.md` is the runbook; when it and a workflow
disagree, the workflow wins.

```
1. Publish the draft        → creates the tag vX.Y.Z-st and the release note. No build.
2. Merge the version-sync PR → Android versionName follows the tag.
3. Actions → Distribute      → the build reads its version from that tag.
```

Usage: `/tag` — prepares the release and asks before anything is published or built.

## What decides the version

You do not compute or push a tag by hand. **Release Drafter** keeps a draft
`ST-Release vX.Y.Z-st` up to date on every push to `main`. Its version comes from the
**labels** on the PRs merged since the last release; the highest bump wins:

| PR title | Label (autolabeler) | Bump on the 0.x line |
|---|---|---|
| `feat(scope): …` | `feature` | minor |
| `fix(scope): …` / `perf: …` | `fix` | patch |
| `feat!: …` / `BREAKING CHANGE` | `breaking` | minor (capped) |
| `chore/docs/refactor/test/ci/build: …` | `chore` | patch |
| no label | — | patch, and the entry lands uncategorised |

Betas stay on `0.x`. Every automatic bump is capped at **minor**; `1.0.0` is reserved
for the first App Store release and is published by hand at launch.

## Steps

1. **Start from a fresh `main`.**
   ```bash
   git fetch origin --tags --prune
   git switch main && git pull --ff-only origin main
   git describe --tags --abbrev=0 origin/main   # what a build would ship right now
   ```

2. **Check the staging draft is complete.**
   ```bash
   gh release list --limit 3                                  # draft ST-Release vX.Y.Z-st on top
   gh release view vX.Y.Z-st --json body --jq .body
   git log --oneline "$(git describe --tags --abbrev=0 origin/main)"..origin/main
   ```
   - Every merged PR since the last tag must appear in the note. If one is missing, re-run
     the drafter: `gh workflow run release-drafter.yml --ref main`.
   - **A green drafter run can still have updated nothing.** When GitHub's GraphQL API
     fails (`##[error] Request failed … Something went wrong while executing your query`),
     the job still reports success, and the draft keeps its old body. That happened
     for `v0.9.0-st`: the PROD draft picked up #239 and the ST draft did not.
     Check the draft's `updated_at` and the job log:
     ```bash
     gh api repos/SJinKim/hanmaum-dn-mobile-app/releases --jq '.[] | select(.draft) | "\(.tag_name) \(.updated_at)"'
     gh run view <id> --log | grep -P "^draft-staging\t" | grep "##\[error\]"
     ```
     Re-run the drafter. If it keeps failing, edit the note by hand, using the PR
     list from `git log`.
   - Nothing may sit above `## 🚀 Features`. An entry there has no category label.
     Label the PR (`gh pr edit <n> --add-label chore`) and re-run the drafter. The
     `version-sync` bot PRs arrive unlabelled.
   - A PR that was opened against another branch (a stacked PR) never passed through the
     autolabeler, which only runs on PRs into `main`. Check that its label is there.
   - The draft's version must sort **above** `git describe` from step 1.

3. **Confirm with the user** before publishing:
   - the version (`vX.Y.Z-st`),
   - the lane (`ios-testflight`, `android-st`, or both).

   Every dispatch spends a real build. When the user does not name a lane, follow what
   recent releases did: `gh run list --workflow=distribute.yml --limit 5` and the job names
   of those runs. Production lanes (`ios-appstore`, `android-prod`) run only on an
   explicit request.

   If the release carries a feature that needs a new server endpoint, check that the
   server deploy to staging has finished. Otherwise testers see the feature's
   unavailable state.

4. **Publish the draft.** This creates the tag and the note, and starts no build.
   Edit the note first if it reads like a commit dump.
   ```bash
   gh release edit vX.Y.Z-st --draft=false
   git fetch --tags origin && git describe --tags --abbrev=0 origin/main   # must now print vX.Y.Z-st
   ```

5. **Merge the version-sync PR.** Publishing opens `chore(release): set versionName to X.Y.Z`.
   No checks are required on `main`, so `--auto` merges immediately. To merge on green:
   ```bash
   gh pr list --search "set versionName to X.Y.Z" --state open
   gh pr checks <n> --watch --fail-fast && gh pr merge <n> --squash --delete-branch
   ```

6. **Dispatch the build**, one run per lane:
   ```bash
   gh workflow run distribute.yml --ref main -f target=ios-testflight
   gh run list --workflow=distribute.yml --limit 1   # confirm it started
   ```
   The marketing version is the tag minus `v` and `-st`. The build number is the run number.

7. **After the run**, confirm `gh run view <id>` succeeded. The user checks the
   build on TestFlight; do not launch the simulator in its place.

## Never

- **Dispatch before publishing.** The build falls back to the previous tag, and testers
  get different content under the same version. That is how builds 76–79 all shipped
  as 0.7.1.
- **Push a tag by hand** for a normal release, or re-point an existing tag. Builds already
  in App Store Connect keep their version.
- **Skip publishing when the draft is empty.** An empty draft means the last publish already covered `main`.
