# Face ID: OS-enforced vault holding the refresh token

**Goal:** Option B of #200. Face ID stops being a UI gate the app can be talked
past, and stops guarding a stored account password.

**Why now:** #197 made the toggle reach the auth flow again. That exposed what
it actually guards: `IosSecureStore` is a plain Keychain item with no
`SecAccessControl`, `AndroidSecureStore` a Keystore key without
`setUserAuthenticationRequired`, and the value behind them is the member's
cleartext password. The prompt returns a boolean the app then trusts — OWASP's
"event-based authentication", bypassable by hooking on a rooted device.

## Design

The secret moves from *password* to *refresh token*, and from *app-enforced* to
*OS-enforced* protection.

```
enable in 설정   →  biometric prompt  →  refresh token sealed into the vault
login screen     →  biometric prompt  →  OS releases refresh token
                 →  refresh_token grant → new session → Home
```

No password is typed to enable it, because the session already exists — this is
what removes the password sheet added in #199.

| | before | after |
| --- | --- | --- |
| stored secret | email + password | refresh token |
| iOS protection | Keychain, no access control | `kSecAccessControlBiometryCurrentSet` |
| Android protection | Keystore key, no auth required | `setUserAuthenticationRequired(true)` + `setInvalidatedByBiometricEnrollment(true)`, unlocked via `BiometricPrompt` + `CryptoObject` |
| biometric class | `BIOMETRIC_WEAK` | `BIOMETRIC_STRONG` (crypto-bound keys require Class 3) |
| bypassable by hooking | yes | no — without biometrics no plaintext is returned |

**Refresh token expiry** is the accepted trade-off: when Keycloak's idle timeout
has passed, the vault still opens but the grant fails, and the member signs in
with their password as usual. That is the mainstream behaviour; avoiding it
needs server-side device binding (option C in #200), which is out of scope.

## Global constraints

- Test gate `./gradlew :composeApp:testDevDebugUnitTest`; iOS
  `iosSimulatorArm64Test` + the xcodebuild interop build (platform code changes).
- No bare `TODO` anywhere in `composeApp/src`.
- User-facing strings: `AppStrings` + Ko + En + De. ViewModel-internal errors:
  hardcoded Korean.
- Test names: letters, digits, spaces only.
- No AI trailers in commits.

## Tasks

- [x] **1 — `BiometricVault` expect/actual.** `core/security/BiometricVault.kt`:
      `isAvailable()`, `hasSecret()`, `suspend seal(secret, prompt…)`,
      `suspend open(prompt…)`, `clear()`, returning a sealed `VaultResult`
      (`Success`/`Cancelled`/`Invalidated`/`Unavailable`/`Empty`/`Failed`).
      `Invalidated` is its own case: re-enrolled biometrics must lead to a
      re-activation prompt, not a silent failure.
- [x] **2 — iOS actual.** `SecAccessControlCreateWithFlags` with
      `kSecAccessControlBiometryCurrentSet`; `SecItemAdd` / `SecItemCopyMatching`
      with an `LAContext`. `errSecUserCanceled` → `Cancelled`,
      `errSecAuthFailed` → `Invalidated`.
- [x] **3 — Android actual.** Keystore AES/GCM key as above; encrypt and decrypt
      through `BiometricPrompt` with a `CryptoObject`.
      `KeyPermanentlyInvalidatedException` → `Invalidated`.
- [x] **4 — Refresh grant in the repository.** `AuthRepository.refresh(token)`
      using the existing Keycloak `refresh_token` grant, so the login screen can
      trade the released token for a session.
- [x] **5 — Rewire enable/disable.** `FaceIdSetupViewModel` seals the refresh
      token instead of verifying a password; the password sheet goes.
- [x] **6 — Rewire sign-in.** `LoginViewModel` opens the vault and refreshes
      instead of replaying credentials.
- [x] **7 — Retire `CredentialStore`** and its password keys.
- [x] **8 — Tests.** Fake vault: seal/open happy path, cancelled, invalidated,
      expired refresh token falls back to the password form.

## Not verifiable here

Neither simulator has enrolled biometrics and there is no test account, so the
prompts themselves are covered by fakes only. The platform actuals need a run on
real hardware before this is trusted.
