# Design: Face ID / Biometric App Lock

**Date:** 2026-06-13
**Branch:** `fix/ministry-and-auto-refresh` (current working branch)
**Status:** Approved decisions — pending spec review

## 1. Goal

Let users turn a biometric **app lock** on/off for the app. When enabled, the app
requires Face ID / Touch ID (iOS) or fingerprint/face (Android `BiometricPrompt`)
to be revealed — on cold launch **and** when returning from the background.

**Decisions taken (with the user):**
- **Security model: UI-gate.** Biometric success reveals the already-persisted
  session; tokens are not crypto-bound to a Keystore/Keychain key. This matches
  the app's current at-rest posture (tokens live in plaintext
  `multiplatform-settings`) and is proportional to a community app. Crypto-binding
  (encrypting the refresh token with a biometric-protected key) is noted as future
  hardening, out of scope here.
- **Lock timing: launch + on resume.** A true app-lock: locks on cold start and
  re-locks when the app goes to the background, re-prompting on next foreground.
- **Implementation: hand-rolled `expect/actual`** (no `moko-biometry`, which would
  drag in `moko-resources` and clash with the existing `LocalStrings` i18n).
  Android: `androidx.biometric`. iOS: built-in `LocalAuthentication`.

## 2. Architecture

### 2.1 Biometric abstraction (commonMain)
`core/security/BiometricAuthenticator.kt`:
```kotlin
enum class BiometricResult { SUCCESS, FAILED, CANCELLED, UNAVAILABLE }

expect class BiometricAuthenticator {
    fun isAvailable(): Boolean                       // hardware present + enrolled
    suspend fun authenticate(
        title: String, subtitle: String, cancelLabel: String,
    ): BiometricResult
}
```
Because Android's `BiometricPrompt` needs a live `FragmentActivity`, the
authenticator is obtained + bound inside Compose:
```kotlin
@Composable expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
```
- **Android actual:** `rememberBiometricAuthenticator()` reads the current
  `Activity` (`LocalActivity`/context cast to `FragmentActivity`) and returns an
  authenticator bound to it. `isAvailable()` uses `BiometricManager.canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG)`. `authenticate()` shows a
  `BiometricPrompt` and resumes a coroutine from its callback. (`BIOMETRIC_WEAK`
  is acceptable for a UI-gate; matches the chosen model.)
- **iOS actual:** wraps `LAContext`. `isAvailable()` =
  `canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics)`.
  `authenticate()` calls `evaluatePolicy(...)` and bridges the completion handler
  via `suspendCancellableCoroutine`. `rememberBiometricAuthenticator()` is a plain
  `remember { BiometricAuthenticator() }` (no binding needed).

### 2.2 Enabled flag (persistence)
Add to the existing settings-backed `TokenStorage` (already holds the related
`keepSignedIn` session pref):
```kotlin
fun setBiometricEnabled(value: Boolean)
fun isBiometricEnabled(): Boolean    // default false (opt-in)
```
Key `"biometric_enabled"`. `clear()` also removes it (logout resets the lock).

### 2.3 Lock gate (App root)
In `App.kt`, wrap the existing `NavHost`:
- `val biometric = rememberBiometricAuthenticator()`
- `var locked by remember { mutableStateOf(initialLocked) }` where
  `initialLocked = tokenStorage.isBiometricEnabled() && tokenStorage.getAccessToken() != null`.
- Re-lock on background: `LifecycleStopEffect`/`LifecycleEventEffect(ON_STOP)` →
  `if (tokenStorage.isBiometricEnabled() && session exists) locked = true`
  (using `androidx.lifecycle.compose`, already on the lifecycle 2.9.6 line).
- When `locked` is true, render a `LockScreen` overlay **above** the NavHost and
  auto-trigger `biometric.authenticate(...)` via `LaunchedEffect(locked)`.
  - `SUCCESS` → `locked = false`.
  - `CANCELLED`/`FAILED` → stay locked; `LockScreen` shows a "Unlock" retry button
    and a "Use password" action that logs out (clear tokens, navigate to Login,
    `locked = false`).
- The lock is only ever shown for an authenticated session; Login/Splash/Pending
  flows are unaffected when no session exists.

`LockScreen` (new, `core/presentation/components/LockScreen.kt`): tinted DN logo
on `surface`, a title, an "Unlock with Face ID" button, and a subtle "Use
password" text button. DESIGN.md tokens; spring press on the button.

### 2.4 Toggle UI (Profile)
A `Switch` row in Profile's "Account Preferences" section, labelled from i18n
(`profileAppLock`). Behaviour:
- If `!biometric.isAvailable()` → row disabled with a hint (`appLockUnavailable`).
- Turning **on**: first call `authenticate(...)`; only persist
  `setBiometricEnabled(true)` on `SUCCESS` (proves the user can pass the check).
- Turning **off**: persist `setBiometricEnabled(false)` (no auth required to relax).
- State is hoisted in `App.kt` like `theme`/`locale` and passed to `ProfileScreen`
  (`biometricEnabled` + `onBiometricToggle`), reusing the bound authenticator.

### 2.5 Platform glue
- **Android:** `MainActivity : ComponentActivity` → `FragmentActivity`
  (`androidx.fragment.app.FragmentActivity`, which extends `ComponentActivity`, so
  `setContent` + `enableEdgeToEdge` are unaffected). Add `androidx.biometric`
  (`androidx.biometric:biometric:1.1.0`) + `androidx.fragment` to `androidMain`.
- **iOS:** add to `iosApp/iosApp/Info.plist`:
  `NSFaceIDUsageDescription` = a Korean usage string (matches existing plist
  language) explaining Face ID unlocks the app.

### 2.6 i18n
Add to `AppStrings` + En/Ko/De: `profileAppLock`, `appLockUnavailable`,
`lockTitle`, `lockUnlockButton`, `lockUsePassword`, `lockSubtitle`.

## 3. Out of scope (YAGNI)
- Crypto-bound token encryption / Keystore-Keychain key (future hardening).
- Idle-timeout tuning (re-lock is on background, not on a timer).
- PIN/passcode fallback UI beyond the system prompt's device-credential option.
- Remembering biometric across reinstalls.

## 4. Verification
- Unit test: `TokenStorageImplTest` covers `biometricEnabled` default/round-trip/clear.
- Biometric flows require a device/simulator (logic can't be JVM-unit-tested):
  - iOS Simulator: Features → Face ID → Enrolled, then Matching/Non-matching Face.
  - Android emulator: add a fingerprint, `adb -e emu finger touch`.
- Build: `:composeApp:assembleDevDebug`, `:composeApp:testDevDebugUnitTest`, `lint`.
- Manual matrix: toggle on (requires auth) → background→foreground re-locks →
  cancel keeps locked → "Use password" logs out → toggle off removes the gate;
  device without enrolled biometrics shows the disabled toggle.

## 5. Risks / notes
- **FragmentActivity switch** is the main Android risk; verify Compose still hosts
  and edge-to-edge still applies after the change.
- **iOS LocalAuthentication interop** must be validated on the simulator (per repo
  practice, reproduce locally rather than via TestFlight).
- UI-gate is intentionally not rooted/jailbroken-resistant; documented as such.
