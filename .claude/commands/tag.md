# /tag — Cut a Release Tag (→ iOS TestFlight)

Create the next semver tag from the latest one and push it. Pushing a `v*` tag
triggers `.github/workflows/distribute.yml` → **iOS TestFlight** (staging backend).
The build number (`CFBundleVersion`) is the GitHub run number; the marketing
version (`CFBundleShortVersionString`) is this tag minus the `v`.

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
   with the user before pushing (a tag push spends a real TestFlight build).
4. **Create an annotated tag on `main` HEAD and push:**
   ```bash
   git tag -a "$NEXT" -m "$NEXT — <one-line summary of what ships>"
   git push origin "$NEXT"
   ```
5. **Verify the build kicked off:**
   ```bash
   gh run list --workflow=distribute.yml --limit 2
   ```

## Notes
- **Production** is a separate, manual, `prod`-gated lane — never a tag. Ship it via
  Actions → "Distribute" → `ios-appstore` (after tagging the prod commit `v1.0.0`),
  or `android-prod`. See `distribute.yml`.
- A minor/major bump always **zeroes the lower numbers** (standard semver) — don't
  carry an old PATCH forward.
- If you must re-point a tag, deleting a remote tag does not re-version builds already
  in App Store Connect (they're immutable); only future builds get the new version.
