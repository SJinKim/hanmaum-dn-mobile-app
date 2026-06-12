# Design Spec: ST Environment Run Configuration

**Date:** 2026-05-28  
**Scope:** Android Studio run config only  
**Status:** Approved

---

## Context

The app has three environments defined in `composeApp/build.gradle.kts`:

| Flavor | `BACKEND_URL` | `KEYCLOAK_URL` | `KEYCLOAK_REALM` |
|--------|---------------|----------------|-----------------|
| `dev`  | `http://10.0.2.2:8080` | `http://10.0.2.2:8091` | `hanmaum` |
| `st`   | `https://api.graceops.de` | `https://auth.graceops.de` | `hanmaum-dn-st` |
| `prod` | `https://api.graceops.de` | `https://auth.graceops.de` | `hanmaum-dn-prod` |

`st` = **system test** — real backend on `graceops.de`, dedicated Keycloak realm and database tables.

Both the `st` buildkonfig target and the `st` Android product flavor already exist. What is missing is a run configuration that locks to `stDebug` without requiring a manual Build Variants panel switch.

---

## What to Build

### File: `.run/composeApp-st.run.xml`

A single new file in the already-gitignored `.run/` directory.

**Configuration type:** `AndroidRunConfigurationType` (same as the existing `composeApp` config)

**Key fields:**

| Option | Value | Reason |
|--------|-------|--------|
| `name` | `composeApp [st]` | Distinguishable in the run config dropdown |
| `module` | `hanmaum-dn-mobile-app.composeApp` | Matches the module name Android Studio uses (confirmed from `workspace.xml`) |
| `ANDROID_RUN_CONFIGURATION_SCHEMA_VERSION` | `1` | Modern schema required for variant override (Iguana 2023.2.1+) |
| `SELECTED_BUILD_VARIANT` | `stDebug` | Locks the config to the st flavor + debug build type |
| All other options | Same as existing `composeApp` config | Debugger, profiler, deploy settings unchanged |

**Note:** `.run/` is in `.gitignore` — this file is local-only, consistent with the existing `composeApp` config.

---

## What is NOT in scope

- iOS: no Xcode scheme for `st` — iOS always uses `localhost` for now
- `build.gradle.kts`: no changes — `st` flavor and buildkonfig target are already complete
- `.env`: no changes — `st` uses the same Google Calendar / pCloud keys as `dev`

---

## Verification

After creating the file:
1. Reload project in Android Studio (File → Sync Project with Gradle Files)
2. The `composeApp [st]` config should appear in the run config dropdown
3. Running it should build `stDebug` and install the APK pointed at `https://api.graceops.de` with realm `hanmaum-dn-st`
