# ST Run Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create an Android Studio run configuration that always builds and installs the `stDebug` variant (system-test environment) without requiring a manual Build Variants panel switch.

**Architecture:** A single `.run/` XML file using `AndroidRunConfigurationType` schema v1 with `SELECTED_BUILD_VARIANT=stDebug`. The `st` product flavor and buildkonfig target already exist in `build.gradle.kts` — no Gradle changes needed.

**Tech Stack:** Android Studio Iguana+ run configuration XML (schema v1), Android product flavors, KMP Gradle

---

## File Map

| Action | Path | Purpose |
|--------|------|---------|
| Create | `.run/composeApp-st.run.xml` | Android Studio run config locked to `stDebug` |

---

### Task 1: Create the ST run configuration file

**Files:**
- Create: `.run/composeApp-st.run.xml`
- Reference: `.run/composeApp.run.xml` (existing config — do not modify)
- Reference: `composeApp/build.gradle.kts` lines 48–52 (st buildkonfig target)

- [ ] **Step 1: Confirm the existing run config module name**

Open `.run/composeApp.run.xml` and note the module name. Then open `.idea/workspace.xml` and find the `RunManager` component — the authoritative module name used by the live IDE is:

```
hanmaum-dn-mobile-app.composeApp
```

(The `.run/` file says `hanmaum-dn-mobile-app.composeApp.main` but workspace.xml — which Android Studio writes itself — uses the shorter form. Use the workspace.xml version for the new config.)

- [ ] **Step 2: Create `.run/composeApp-st.run.xml`**

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="composeApp [st]" type="AndroidRunConfigurationType" factoryName="Android App">
    <module name="hanmaum-dn-mobile-app.composeApp" />
    <option name="ANDROID_RUN_CONFIGURATION_SCHEMA_VERSION" value="1" />
    <option name="SELECTED_BUILD_VARIANT" value="stDebug" />
    <option name="DEPLOY" value="true" />
    <option name="DEPLOY_APK_FROM_BUNDLE" value="false" />
    <option name="DEPLOY_AS_INSTANT" value="false" />
    <option name="ARTIFACT_NAME" value="" />
    <option name="PM_INSTALL_OPTIONS" value="" />
    <option name="ALL_USERS" value="false" />
    <option name="ALWAYS_INSTALL_WITH_PM" value="false" />
    <option name="CLEAR_APP_STORAGE" value="false" />
    <option name="DYNAMIC_FEATURES_DISABLED_LIST" value="" />
    <option name="ACTIVITY_EXTRA_FLAGS" value="" />
    <option name="MODE" value="default_activity" />
    <option name="CLEAR_LOGCAT" value="false" />
    <option name="SHOW_LOGCAT_AUTOMATICALLY" value="false" />
    <option name="TARGET_SELECTION_MODE" value="DEVICE_AND_SNAPSHOT_COMBO_BOX" />
    <option name="SELECTED_CLOUD_MATRIX_CONFIGURATION_ID" value="-1" />
    <option name="SELECTED_CLOUD_MATRIX_PROJECT_ID" value="" />
    <option name="DEBUGGER_TYPE" value="Auto" />
    <Auto>
      <option name="USE_JAVA_AWARE_DEBUGGER" value="false" />
      <option name="SHOW_STATIC_VARS" value="true" />
      <option name="WORKING_DIR" value="" />
      <option name="TARGET_LOGGING_CHANNELS" value="lldb process:gdb-remote packets" />
      <option name="SHOW_OPTIMIZED_WARNING" value="true" />
      <option name="ATTACH_ON_WAIT_FOR_DEBUGGER" value="false" />
      <option name="DEBUG_SANDBOX_SDK" value="false" />
    </Auto>
    <Hybrid>
      <option name="USE_JAVA_AWARE_DEBUGGER" value="false" />
      <option name="SHOW_STATIC_VARS" value="true" />
      <option name="WORKING_DIR" value="" />
      <option name="TARGET_LOGGING_CHANNELS" value="lldb process:gdb-remote packets" />
      <option name="SHOW_OPTIMIZED_WARNING" value="true" />
      <option name="ATTACH_ON_WAIT_FOR_DEBUGGER" value="false" />
      <option name="DEBUG_SANDBOX_SDK" value="false" />
    </Hybrid>
    <Java>
      <option name="ATTACH_ON_WAIT_FOR_DEBUGGER" value="false" />
      <option name="DEBUG_SANDBOX_SDK" value="false" />
    </Java>
    <Native>
      <option name="USE_JAVA_AWARE_DEBUGGER" value="false" />
      <option name="SHOW_STATIC_VARS" value="true" />
      <option name="WORKING_DIR" value="" />
      <option name="TARGET_LOGGING_CHANNELS" value="lldb process:gdb-remote packets" />
      <option name="SHOW_OPTIMIZED_WARNING" value="true" />
      <option name="ATTACH_ON_WAIT_FOR_DEBUGGER" value="false" />
      <option name="DEBUG_SANDBOX_SDK" value="false" />
    </Native>
    <Profilers>
      <option name="ADVANCED_PROFILING_ENABLED" value="false" />
      <option name="STARTUP_PROFILING_ENABLED" value="false" />
      <option name="STARTUP_CPU_PROFILING_ENABLED" value="false" />
      <option name="STARTUP_CPU_PROFILING_CONFIGURATION_NAME" value="Java/Kotlin Method Sample (legacy)" />
      <option name="STARTUP_NATIVE_MEMORY_PROFILING_ENABLED" value="false" />
      <option name="NATIVE_MEMORY_SAMPLE_RATE_BYTES" value="2048" />
    </Profilers>
    <option name="DEEP_LINK" value="" />
    <option name="ACTIVITY_CLASS" value="" />
    <option name="SEARCH_ACTIVITY_IN_GLOBAL_SCOPE" value="false" />
    <option name="SKIP_ACTIVITY_VALIDATION" value="false" />
    <method v="2">
      <option name="Android.Gradle.BeforeRunTask" enabled="true" />
    </method>
  </configuration>
</component>
```

- [ ] **Step 3: Verify the XML is well-formed**

Run:
```bash
xmllint --noout .run/composeApp-st.run.xml && echo "XML valid"
```
Expected output: `XML valid`

If `xmllint` is not installed, open the file in any text editor and confirm tags are balanced.

- [ ] **Step 4: Verify Android Studio picks up the config**

In Android Studio:
1. **File → Sync Project with Gradle Files** (or click the elephant icon)
2. Open the run config dropdown (top toolbar)
3. Confirm `composeApp [st]` appears alongside the existing `composeApp` entry
4. Select `composeApp [st]` and check the **Build Variants** panel — it should show `stDebug` selected for `composeApp`

> **If `SELECTED_BUILD_VARIANT` is not honoured** (older Android Studio < Iguana 2023.2.1):  
> The config will still appear and run, but variant selection falls back to the Build Variants panel. In that case, manually select `stDebug` in Build Variants once and the config will use it.

- [ ] **Step 5: Smoke-test on emulator**

Select `composeApp [st]` in the run config dropdown, pick the Pixel 7 Pro emulator, and press Run.

After the app launches, verify it connects to the ST environment:
- Login screen should accept ST Keycloak credentials (realm `hanmaum-dn-st`)
- Network requests in Logcat should show `https://api.graceops.de` as the base URL (filter tag `OkHttp`)

- [ ] **Step 6: No commit needed**

`.run/` is in `.gitignore` — the file is intentionally local-only. No git action required.
