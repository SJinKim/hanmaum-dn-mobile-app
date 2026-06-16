# Tag Command — Cut a Release Tag (→ iOS TestFlight)

Use to create the next release tag from the latest one and push it. Pushing a
`v*` tag triggers `.github/workflows/distribute.yml` → iOS TestFlight (staging
backend). The marketing version (`CFBundleShortVersionString`) is the tag minus
the `v`; the build number (`CFBundleVersion`) is the GitHub run number.

Bump rules for `vMAJOR.MINOR.PATCH`:

- **feature** → increment MINOR (2nd), reset PATCH to 0 — `0.2.1 → 0.3.0`
- **fix** → increment PATCH (3rd) — `0.2.0 → 0.2.1`
- **major** → increment MAJOR (1st), reset MINOR + PATCH to 0 — `0.9.3 → 1.0.0`

Versioning convention: betas ship on the `0.x` line. `1.0.0` is reserved for the
first production (App Store) release — do not cross into `1.x` with a `major`
bump unless you intend that launch. TestFlight beta versions do not constrain the
App Store version (no released App Store version exists yet).

Steps:

1. Tag the merged commit on `develop`, never a feature branch:
   ```bash
   git fetch origin --tags --prune
   git checkout develop && git pull --ff-only origin develop
   ```
2. Find the latest semver tag (ignore suffixed tags like `-prototype`):
   ```bash
   LATEST=$(git tag -l 'v*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1)
   ```
3. Compute the next tag per the bump rules. A minor/major bump always zeroes the
   lower numbers. Infer the bump from commits since the last tag (`feat:` →
   feature, `fix:` → fix) if not given, and confirm the value before pushing —
   a tag push spends a real TestFlight build.
4. Create an annotated tag on `develop` HEAD and push:
   ```bash
   git tag -a "$NEXT" -m "$NEXT — <one-line summary of what ships>"
   git push origin "$NEXT"
   ```
5. Verify the build started: `gh run list --workflow=distribute.yml --limit 2`.

Notes:

- Production is a separate, manual, `prod`-gated lane — never a tag. Ship it via
  Actions → "Distribute" → `ios-appstore` (after tagging the prod commit
  `v1.0.0`) or `android-prod`. See `distribute.yml`.
- Deleting a remote tag does not re-version builds already in App Store Connect
  (they are immutable); only future builds get the new version.
- Never add `Co-Authored-By:` trailers when committing related changes.
