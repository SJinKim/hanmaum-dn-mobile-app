# /tag — Cut a Release Tag

Create the next semver tag from the latest one and push it.

**A tag no longer starts a build.** Since every distribution became a manual
dispatch, a tag only *marks* a version: `distribute.yml` reads the newest one to
fill `CFBundleShortVersionString` (tag minus the `v`), while `CFBundleVersion` is
the GitHub run number. Shipping is a separate, deliberate step — Actions →
"Distribute" → Run workflow.

Without a fresh tag a build simply reuses the last tag's version and only the
build number climbs: `0.6.0 (12)`, then `0.6.0 (13)`. That is fine for iterating,
but tag before anything a tester should be able to tell apart.

Usage: `/tag feature` | `/tag fix` | `/tag major`
If no argument is given, infer the bump from the commits since the last tag
(`feat:` → feature, `fix:` → fix) and state the choice before tagging.

## Bump Rules — `vMAJOR.MINOR.PATCH`
- **feature** → increment **MINOR** (2nd number), reset PATCH to 0 — e.g. `0.2.1 → 0.3.0`
- **fix** → increment **PATCH** (3rd number) — e.g. `0.2.0 → 0.2.1`
- **major** → increment **MAJOR** (1st number), reset MINOR + PATCH to 0 — e.g. `0.9.3 → 1.0.0`

> **Versioning convention:** betas ship on the **`0.x` line**. `1.0.0` is reserved
> for the **first production (App Store) release** — do NOT cross into `1.x` with a
> `major` bump unless you intend that production launch. TestFlight beta versions do
> not constrain the App Store version (no released App Store version exists yet).

## Steps
1. **Be on the mainline.** Tag the merged commit on `main`, never a feature branch:
   ```bash
   git fetch origin --tags --prune
   git checkout main && git pull --ff-only origin main
   ```
2. **Find the latest semver tag** (ignore suffixed tags like `-prototype`):
   ```bash
   LATEST=$(git tag -l 'v*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1)
   echo "Latest: ${LATEST:-<none>}"   # falls back to v0.0.0 if empty
   ```
3. **Compute the next tag** per the bump rules above. Confirm the computed value
   with the user before pushing — a tag is cheap now, but it is still a public
   marker of what shipped.
4. **Create an annotated tag on `main` HEAD and push:**
   ```bash
   git tag -a "$NEXT" -m "$NEXT — <one-line summary of what ships>"
   git push origin "$NEXT"
   ```
5. **Ship it, if that is the intent.** The tag alone changes nothing:
   ```bash
   gh workflow run distribute.yml -f target=ios-testflight
   gh run list --workflow=distribute.yml --limit 2
   ```

## Notes
- **Every lane is manual**, production included: `android-st`, `ios-testflight`,
  `android-prod`, `ios-appstore`. The production ones are gated by the `prod`
  environment's approval. See `distribute.yml`.
- **Publishing a release draft also creates its tag** — that is the usual route.
  `/tag` is the hand-rolled alternative when there is no draft to publish.
- A minor/major bump always **zeroes the lower numbers** (standard semver) — don't
  carry an old PATCH forward.
- If you must re-point a tag, deleting a remote tag does not re-version builds already
  in App Store Connect (they're immutable); only future builds get the new version.
