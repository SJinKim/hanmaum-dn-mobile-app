# Secure Token Storage (L1 + L2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move auth tokens out of plaintext `SharedPreferences`/`NSUserDefaults` into hardware-backed secure storage (Stage 1 / L1), then crypto-gate the offline refresh token behind OS biometrics-or-passcode with an in-memory foreground session so silent refresh never prompts (Stage 2 / L2).

**Architecture:** Two stages, each independently shippable.
- **Stage 1 (L1):** A platform-provided *secure* `Settings` (iOS `KeychainSettings`, Android `EncryptedSharedPreferences`) backs `TokenStorageImpl`. Pure storage-backend swap — zero behaviour change. Ship this first.
- **Stage 2 (L2):** The refresh/offline token moves to a dedicated biometric-gated store (`BiometricRefreshStore`, expect/actual): **free to write** (so silent token rotation never prompts), **gated to read** (OS biometric/passcode). A common `RefreshTokenVault` caches the unlocked token in memory for the foreground session; `TokenStorage.getRefreshToken()` returns the in-memory copy only. The lock screen's unlock action *is* the gated read (one prompt, no double-prompt). Access token stays in L1 so background refreshes work.

**Tech Stack:** Kotlin Multiplatform, `com.russhwolf:multiplatform-settings` 1.3.0 (`KeychainSettings`), `androidx.security:security-crypto` (EncryptedSharedPreferences), Android Keystore (RSA-2048 hybrid), `androidx.biometric:biometric` 1.1.0 (`BiometricPrompt` + `CryptoObject`), iOS Keychain `SecItem` + `SecAccessControl`, Koin DI, Ktor auth refresh.

**Key product decision (Stage 2):** When "Keep me signed in" is on, the offline token is gated behind device auth (biometric **or** device passcode — `LAPolicyDeviceOwnerAuthentication` / `DEVICE_CREDENTIAL`). This means a stay-signed-in session shows one auth prompt on cold start / re-foreground. Devices with no passcode at all fall back to L1 (encrypted, ungated) so the app still works. This deliberately supersedes the old opt-in app-lock for stay-signed-in sessions; the existing biometric toggle remains only as a UI preference and is no longer what protects the token.

---

## File Structure

**Stage 1 (L1):**
- Modify `gradle/libs.versions.toml` — add `androidx-security-crypto`.
- Modify `composeApp/build.gradle.kts` — add the Android dep.
- Create `composeApp/src/commonMain/.../core/data/repository/SecureSettings.kt` — `expect fun secureSettings(): Settings`.
- Create `composeApp/src/androidMain/.../core/data/repository/SecureSettings.android.kt` — EncryptedSharedPreferences actual (needs `Context`).
- Create `composeApp/src/iosMain/.../core/data/repository/SecureSettings.ios.kt` — `KeychainSettings` actual.
- Modify `composeApp/src/.../di/PlatformModule.{android,ios}.kt` — provide `single(named("secure")) { ... Settings }`.
- Modify `composeApp/src/.../di/AppModule.kt` — `TokenStorageImpl(get(named("secure")))`.

**Stage 2 (L2):**
- Create `core/security/BiometricRefreshStore.kt` (expect) + `.android.kt` + `.ios.kt` — gated secure store for the refresh token.
- Create `core/security/RefreshTokenVault.kt` (common) — in-memory session + persistence orchestration.
- Create `core/security/RefreshTokenVaultTest.kt` (commonTest) — TDD the common logic.
- Create `core/security/RefreshTokenUnlocker.kt` (expect, `@Composable`) + `.android.kt` + `.ios.kt` — the prompt-and-decrypt action.
- Modify `core/domain/repository/TokenStorage.kt` + `core/data/repository/TokenStorageImpl.kt` — route refresh token through the vault.
- Modify `core/network/NetworkClient.kt` — unchanged logic, but now reads in-memory refresh token (verify).
- Modify `App.kt` — lock state driven by the vault; unlock uses `RefreshTokenUnlocker`; `ON_STOP` calls `vault.lock()`.
- Modify `di/AppModule.kt` + `di/PlatformModule.*` — wire vault + store.

---

# STAGE 1 — L1: Encrypt tokens at rest

### Task 1: Add the Android security-crypto dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts:122-134` (androidMain deps)

- [ ] **Step 1: Add version + library to the catalog**

In `gradle/libs.versions.toml`, under `[versions]` add:

```toml
androidx-security-crypto = "1.1.0-alpha06"
```

Under `[libraries]` add:

```toml
androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "androidx-security-crypto" }
```

- [ ] **Step 2: Add the dependency to androidMain**

In `composeApp/build.gradle.kts`, inside `androidMain.dependencies { ... }` add:

```kotlin
            implementation(libs.androidx.security.crypto)
```

- [ ] **Step 3: Verify it resolves**

Run: `./gradlew :composeApp:dependencies --configuration devDebugRuntimeClasspath | grep -i security-crypto`
Expected: a line resolving `androidx.security:security-crypto:1.1.0-alpha06`.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "build: add androidx security-crypto for encrypted token storage"
```

---

### Task 2: `secureSettings()` expect/actual

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/SecureSettings.kt`
- Create: `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/SecureSettings.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/SecureSettings.ios.kt`

> No unit test: a `Settings` backed by Keychain/Keystore cannot run on the JVM test target. Verified by build + on-device check in Task 4.

- [ ] **Step 1: Write the common expect**

Create `SecureSettings.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.data.repository

import com.russhwolf.settings.Settings

/**
 * A [Settings] instance backed by the platform's hardware-backed secure store —
 * iOS Keychain, Android EncryptedSharedPreferences. Used only for secrets
 * (auth tokens); non-secret prefs (locale, theme) keep the plain `Settings()`.
 *
 * On Android the implementation needs a [android.content.Context], so the actual
 * is created inside the Koin platform module (which has `androidContext()`),
 * not as a no-arg factory.
 */
expect class SecureSettingsFactory {
    fun create(): Settings
}
```

- [ ] **Step 2: Write the Android actual**

Create `SecureSettings.android.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual class SecureSettingsFactory(private val context: Context) {
    actual fun create(): Settings {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        return SharedPreferencesSettings(prefs)
    }

    private companion object {
        const val PREFS_NAME = "dn_secure_tokens"
    }
}
```

- [ ] **Step 3: Write the iOS actual**

Create `SecureSettings.ios.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.data.repository

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

@OptIn(ExperimentalSettingsImplementation::class)
actual class SecureSettingsFactory {
    actual fun create(): Settings = KeychainSettings(service = SERVICE)

    private companion object {
        const val SERVICE = "com.hanmaum.dn.mobile.tokens"
    }
}
```

- [ ] **Step 4: Verify both targets compile**

Run: `./gradlew :composeApp:compileDevDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL. If iOS reports `KeychainSettings` unresolved, add `implementation(libs.multiplatform.settings)` (the core artifact) to `commonMain.dependencies` and re-run.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/SecureSettings.kt \
        composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/SecureSettings.android.kt \
        composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/SecureSettings.ios.kt
git commit -m "feat(security): platform secure Settings factory (Keychain / EncryptedSharedPreferences)"
```

---

### Task 3: Wire `TokenStorageImpl` onto the secure store

**Files:**
- Modify: `composeApp/src/androidMain/.../di/PlatformModule.android.kt:10-13`
- Modify: `composeApp/src/iosMain/.../di/PlatformModule.ios.kt:9-12`
- Modify: `composeApp/src/commonMain/.../di/AppModule.kt:61` (the `TokenStorage` binding)

- [ ] **Step 1: Provide the secure Settings in the Android platform module**

In `PlatformModule.android.kt`, add imports and a named binding:

```kotlin
import com.hanmaum.dn.mobile.core.data.repository.SecureSettingsFactory
import com.russhwolf.settings.Settings
import org.koin.core.qualifier.named
```

Inside `module { ... }` add:

```kotlin
    single(named("secure")) { SecureSettingsFactory(androidContext()).create() }
```

- [ ] **Step 2: Provide the secure Settings in the iOS platform module**

In `PlatformModule.ios.kt`, add imports and binding:

```kotlin
import com.hanmaum.dn.mobile.core.data.repository.SecureSettingsFactory
import com.russhwolf.settings.Settings
import org.koin.core.qualifier.named
```

Inside `module { ... }` add:

```kotlin
    single(named("secure")) { SecureSettingsFactory().create() }
```

- [ ] **Step 3: Point `TokenStorage` at the secure Settings**

In `AppModule.kt`, add import:

```kotlin
import org.koin.core.qualifier.named
```

Change the binding at `:61` from:

```kotlin
    single<TokenStorage> { TokenStorageImpl(Settings()) }
```

to:

```kotlin
    single<TokenStorage> { TokenStorageImpl(get(named("secure"))) }
```

(Leave the `LocaleRepository` / `ThemeRepository` bindings on the plain `Settings()` — those are not secrets.)

- [ ] **Step 4: Verify the existing token tests still pass**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "*TokenStorageImplTest"`
Expected: PASS (the interface is unchanged; `TokenStorageImplTest` uses `MapSettings` directly and is unaffected).

- [ ] **Step 5: Build both targets**

Run: `./gradlew :composeApp:assembleDevDebug :composeApp:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/di/PlatformModule.android.kt \
        composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/di/PlatformModule.ios.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt
git commit -m "feat(security): store auth tokens in hardware-backed secure storage (L1)"
```

---

### Task 4: Manual on-device verification (L1)

**Files:** none (verification only).

- [ ] **Step 1: Android — confirm tokens are encrypted at rest**

Run on a connected device/emulator: `./gradlew :composeApp:installDevDebug`, log in with "Keep me signed in", then:

Run: `adb shell run-as com.hanmaum.dn.mobile cat /data/data/com.hanmaum.dn.mobile/shared_prefs/dn_secure_tokens.xml`
Expected: keys/values are **base64 ciphertext**, not the raw JWT. Kill and relaunch the app → it still auto-logs-in (token survived).

- [ ] **Step 2: iOS — confirm app still authenticates after relaunch**

Build/run on the simulator (per `tasks/lessons.md` recipe), log in, terminate, relaunch → still signed in. (Keychain contents aren't shell-inspectable, but a successful relaunch + a Keychain-backed `KeychainSettings` confirms the path.)

- [ ] **Step 3: Tag the shippable point**

```bash
git tag l1-secure-storage
```

> **Stage 1 is independently shippable here.** Open a PR for Stage 1 alone if you want the at-rest-encryption win in production immediately, then continue to Stage 2.

---

# STAGE 2 — L2: Biometric-gated offline refresh token

### Task 5: `BiometricRefreshStore` expect + common contract

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricRefreshStore.kt`

> Contract only here; platform actuals in Tasks 6–7. The *write* path is synchronous & ungated; the *read* path is performed by `RefreshTokenUnlocker` (Task 8) because it must show a UI prompt.

- [ ] **Step 1: Write the common expect**

Create `BiometricRefreshStore.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.security

/**
 * Hardware-backed store for the offline refresh token.
 *
 * - [store] / [delete] / [hasStored] are synchronous and require NO user auth, so
 *   silent token rotation can persist a freshly-issued refresh token without ever
 *   prompting (iOS: free Keychain write; Android: RSA public-key encrypt).
 * - The *read* is gated behind device auth (biometric or passcode) and is performed
 *   by [RefreshTokenUnlocker], which owns the platform prompt.
 *
 * On Android the read uses an RSA private key in the Keystore created with
 * `setUserAuthenticationRequired(true)`; [cipherForUnlock] returns the Cipher that
 * `BiometricPrompt` authorizes, and [decryptAfterAuth] finishes the decryption.
 * On iOS the read is a Keychain lookup whose `SecAccessControl` self-prompts, so the
 * Android-only members below are no-op/unused there.
 */
expect class BiometricRefreshStore {
    /** True if a device secret (biometric or passcode) exists so gating is possible. */
    fun isDeviceSecured(): Boolean

    fun hasStored(): Boolean

    /** Persist (encrypt) the refresh token. No prompt. */
    fun store(token: String)

    fun delete()
}
```

- [ ] **Step 2: Verify it compiles (expect with no actuals yet will fail — that's expected; proceed to Task 6 before building)**

No build here. Continue.

---

### Task 6: Android `BiometricRefreshStore` (Keystore RSA-hybrid)

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricRefreshStore.android.kt`

> RSA-2048 cannot directly encrypt a multi-hundred-byte JWT, so this uses hybrid encryption: a random AES-256-GCM key encrypts the token; the RSA public key (free) wraps the AES key; the RSA private key (auth-gated) unwraps it. Persisted blobs live in a plain prefs file — they are already ciphertext.

- [ ] **Step 1: Write the actual**

Create `BiometricRefreshStore.android.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

actual class BiometricRefreshStore(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    actual fun isDeviceSecured(): Boolean {
        val km = context.getSystemService(android.app.KeyguardManager::class.java)
        return km?.isDeviceSecure == true
    }

    actual fun hasStored(): Boolean = prefs.contains(KEY_CIPHERTEXT)

    actual fun store(token: String) {
        ensureKeyPair()
        // 1. Random AES-256 key encrypts the token (GCM).
        val aesKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, aesKey) }
        val ciphertext = aesCipher.doFinal(token.encodeToByteArray())
        val iv = aesCipher.iv
        // 2. RSA public key (no auth) wraps the AES key.
        val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey
        val rsaCipher = Cipher.getInstance(RSA_TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, publicKey) }
        val wrappedKey = rsaCipher.doFinal(aesKey.encoded)
        prefs.edit()
            .putString(KEY_CIPHERTEXT, ciphertext.b64())
            .putString(KEY_IV, iv.b64())
            .putString(KEY_WRAPPED, wrappedKey.b64())
            .apply()
    }

    actual fun delete() {
        prefs.edit().clear().apply()
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    /** Cipher (RSA DECRYPT) that BiometricPrompt must authorize before [decryptAfterAuth]. */
    fun cipherForUnlock(): Cipher {
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as java.security.PrivateKey
        return Cipher.getInstance(RSA_TRANSFORM).apply { init(Cipher.DECRYPT_MODE, privateKey) }
    }

    /** After BiometricPrompt authorizes [authorizedCipher], unwrap the AES key and decrypt. */
    fun decryptAfterAuth(authorizedCipher: Cipher): String? {
        val wrapped = prefs.getString(KEY_WRAPPED, null)?.unb64() ?: return null
        val iv = prefs.getString(KEY_IV, null)?.unb64() ?: return null
        val ciphertext = prefs.getString(KEY_CIPHERTEXT, null)?.unb64() ?: return null
        val aesKeyBytes = authorizedCipher.doFinal(wrapped)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
            .apply { init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv)) }
        return aesCipher.doFinal(ciphertext).decodeToString()
    }

    private fun ensureKeyPair() {
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setKeySize(2048)
            .setUserAuthenticationRequired(true) // gates PRIVATE-key (decrypt) use only
        // Auth valid only for the single gated op; biometric OR device credential.
        builder.setUserAuthenticationParameters(
            0,
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
        )
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        generator.initialize(builder.build())
        generator.generateKeyPair()
    }

    private fun ByteArray.b64() = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.unb64() = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "dn_refresh_token_key"
        const val RSA_TRANSFORM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        const val PREFS = "dn_refresh_secure"
        const val KEY_CIPHERTEXT = "ct"
        const val KEY_IV = "iv"
        const val KEY_WRAPPED = "wk"
    }
}
```

- [ ] **Step 2: Compile Android**

Run: `./gradlew :composeApp:compileDevDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL. (`setUserAuthenticationParameters` requires API 30+ at the type level but compiles against compileSdk 36; a runtime guard for API < 30 is added in Task 9 wiring notes — see Step 3.)

- [ ] **Step 3: Add an API-level guard for `setUserAuthenticationParameters`**

`setUserAuthenticationParameters(int, int)` exists from API 30. minSdk is 24. Replace the unconditional call with:

```kotlin
            .setUserAuthenticationRequired(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1) // -1 = require auth for every use
        }
```

(`-1` on the legacy API means "biometric required per use, no time window" and routes through `BiometricPrompt` with a `CryptoObject`, which is what Task 8 does.)

- [ ] **Step 4: Re-compile Android**

Run: `./gradlew :composeApp:compileDevDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricRefreshStore.android.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricRefreshStore.kt
git commit -m "feat(security): Android Keystore RSA-hybrid store for gated refresh token"
```

---

### Task 7: iOS `BiometricRefreshStore` (Keychain + SecAccessControl)

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricRefreshStore.ios.kt`

> The gated *read* lives here as `read(reason)` because on iOS the Keychain lookup self-prompts via `LAContext`; `RefreshTokenUnlocker` (iOS) just calls it. Write uses `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` + `SecAccessControl(.userPresence)` — biometric OR passcode, this device only, never in backups.

- [ ] **Step 1: Write the actual**

Create `BiometricRefreshStore.ios.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.NSUTF8StringEncoding
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecUseOperationPrompt
import platform.Security.kSecValueData
import platform.Security.kSecAccessControlUserPresence
import platform.Security.errSecSuccess
import platform.darwin.noErr

@OptIn(ExperimentalForeignApi::class)
actual class BiometricRefreshStore {

    actual fun isDeviceSecured(): Boolean = true // userPresence falls back to passcode; assume securable

    actual fun hasStored(): Boolean = memScoped {
        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE,
            kSecAttrAccount to ACCOUNT,
            kSecMatchLimit to kSecMatchLimitOne,
            kSecReturnData to false,
            // Do NOT prompt just to check existence.
        )
        val status = SecItemCopyMatching(query.toCFDictionary(), null)
        status == errSecSuccess
    }

    actual fun store(token: String) {
        delete()
        memScoped {
            val access = SecAccessControlCreateWithFlags(
                null,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                kSecAccessControlUserPresence, // biometric OR device passcode
                null,
            )
            val data = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            val attrs = mutableMapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE,
                kSecAttrAccount to ACCOUNT,
                kSecValueData to data,
                kSecAttrAccessControl to access,
            )
            SecItemAdd(attrs.toCFDictionary(), null)
        }
    }

    /** Gated read — triggers the system biometric/passcode prompt with [reason]. */
    fun read(reason: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE,
            kSecAttrAccount to ACCOUNT,
            kSecMatchLimit to kSecMatchLimitOne,
            kSecReturnData to true,
            kSecUseOperationPrompt to reason,
        )
        val status = SecItemCopyMatching(query.toCFDictionary(), result.ptr)
        if (status != errSecSuccess) return null
        val nsData = CFBridgingRelease(result.value) as? NSData ?: return null
        NSString.create(nsData, NSUTF8StringEncoding) as String?
    }

    actual fun delete() {
        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE,
            kSecAttrAccount to ACCOUNT,
        )
        SecItemDelete(query.toCFDictionary())
    }

    private companion object {
        const val SERVICE = "com.hanmaum.dn.mobile.refresh"
        const val ACCOUNT = "offline_refresh_token"
    }
}
```

- [ ] **Step 2: Add the `toCFDictionary` helper**

The Security APIs take `CFDictionaryRef`. Add a small bridge at the bottom of the same file:

```kotlin
@OptIn(ExperimentalForeignApi::class)
private fun Map<Any?, Any?>.toCFDictionary(): CFDictionaryRef? =
    platform.Foundation.NSDictionary.create(this as Map<Any?, *>).let {
        CFBridgingRetainCompat(it)
    }
```

> Bridging `NSDictionary` ↔ `CFDictionaryRef` via `CFBridgingRetain` is the standard toll-free-bridge path. If the exact symbol name differs on the installed Kotlin/Native, use `interpretCPointer`/`reinterpret` on the `NSDictionary` pointer — this is the one spot that may need a small adjustment against the toolchain.

- [ ] **Step 3: Compile iOS**

Run: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL. If CF bridging symbols don't resolve, adjust per the note in Step 2 (this is the expected friction point on iOS) and re-run.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricRefreshStore.ios.kt
git commit -m "feat(security): iOS Keychain access-control store for gated refresh token"
```

---

### Task 8: `RefreshTokenUnlocker` (the prompt-and-decrypt action)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenUnlocker.kt`
- Create: `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenUnlocker.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenUnlocker.ios.kt`

- [ ] **Step 1: Common expect**

Create `RefreshTokenUnlocker.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable

/** Result of attempting to unlock (read) the gated refresh token. */
sealed interface UnlockResult {
    data class Success(val token: String) : UnlockResult
    data object Cancelled : UnlockResult
    data object Failed : UnlockResult
    /** No token was stored (e.g. fresh install) — caller should route to login. */
    data object Empty : UnlockResult
}

/** Shows the OS biometric/passcode prompt and, on success, returns the decrypted token. */
expect class RefreshTokenUnlocker {
    suspend fun unlock(reason: String): UnlockResult
}

/** Obtain an unlocker bound to the current platform UI context. */
@Composable
expect fun rememberRefreshTokenUnlocker(): RefreshTokenUnlocker
```

- [ ] **Step 2: Android actual (BiometricPrompt + CryptoObject)**

Create `RefreshTokenUnlocker.android.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.security

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.coroutines.resume

actual class RefreshTokenUnlocker(
    private val activity: FragmentActivity,
    private val store: BiometricRefreshStore,
) {
    actual suspend fun unlock(reason: String): UnlockResult = withContext(Dispatchers.Main) {
        if (!store.hasStored()) return@withContext UnlockResult.Empty
        val cipher = runCatching { store.cipherForUnlock() }.getOrNull()
            ?: return@withContext UnlockResult.Failed
        suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val authedCipher = result.cryptoObject?.cipher
                        val token = authedCipher?.let { runCatching { store.decryptAfterAuth(it) }.getOrNull() }
                        if (cont.isActive) {
                            cont.resume(if (token != null) UnlockResult.Success(token) else UnlockResult.Failed)
                        }
                    }

                    override fun onAuthenticationError(code: Int, msg: CharSequence) {
                        val cancelled = code == BiometricPrompt.ERROR_USER_CANCELED ||
                            code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            code == BiometricPrompt.ERROR_CANCELED
                        if (cont.isActive) {
                            cont.resume(if (cancelled) UnlockResult.Cancelled else UnlockResult.Failed)
                        }
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(reason)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()
            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
        }
    }
}

@Composable
actual fun rememberRefreshTokenUnlocker(): RefreshTokenUnlocker {
    val activity = LocalActivity.current as FragmentActivity
    val store = koinInject<BiometricRefreshStore>()
    return remember { RefreshTokenUnlocker(activity, store) }
}
```

> Note: with `DEVICE_CREDENTIAL` allowed, `PromptInfo` must NOT set a negative button (the API enforces this) — hence no `setNegativeButtonText` here.

- [ ] **Step 3: iOS actual (gated Keychain read self-prompts)**

Create `RefreshTokenUnlocker.ios.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

actual class RefreshTokenUnlocker(private val store: BiometricRefreshStore) {
    actual suspend fun unlock(reason: String): UnlockResult = withContext(Dispatchers.Default) {
        if (!store.hasStored()) return@withContext UnlockResult.Empty
        // SecItemCopyMatching with the access-control item shows the system prompt.
        when (val token = store.read(reason)) {
            null -> UnlockResult.Failed // includes user cancel; treated as failed-to-unlock
            else -> UnlockResult.Success(token)
        }
    }
}

@Composable
actual fun rememberRefreshTokenUnlocker(): RefreshTokenUnlocker {
    val store = koinInject<BiometricRefreshStore>()
    return remember { RefreshTokenUnlocker(store) }
}
```

- [ ] **Step 4: Compile both targets**

Run: `./gradlew :composeApp:compileDevDebugKotlinAndroid && DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL both.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenUnlocker.kt \
        composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenUnlocker.android.kt \
        composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenUnlocker.ios.kt
git commit -m "feat(security): biometric/passcode unlocker for the gated refresh token"
```

---

### Task 9: `RefreshTokenVault` — in-memory session + TDD

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenVault.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenVaultTest.kt`

> The vault is the common, unit-testable brain. It holds the unlocked token in memory for the foreground session, delegates persistence to `BiometricRefreshStore`, and is what `TokenStorage.getRefreshToken()` reads (so silent refresh never prompts). We inject the store behind a tiny interface so it can be faked on the JVM.

- [ ] **Step 1: Extract a fakeable interface for the store**

Add to `BiometricRefreshStore.kt` (common) — a persistence interface the vault depends on:

```kotlin
/** The subset of [BiometricRefreshStore] the vault needs; lets tests fake persistence. */
interface RefreshTokenPersistence {
    fun isDeviceSecured(): Boolean
    fun hasStored(): Boolean
    fun store(token: String)
    fun delete()
}
```

And make the expect class implement it by changing the expect declaration header to:

```kotlin
expect class BiometricRefreshStore : RefreshTokenPersistence {
```

(Both actuals already declare matching `isDeviceSecured/hasStored/store/delete` signatures, so they satisfy the interface. Add `: RefreshTokenPersistence` is implied by the expect; no actual change needed beyond confirming the four methods stay `actual`.)

- [ ] **Step 2: Write the failing test**

Create `RefreshTokenVaultTest.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakePersistence(secured: Boolean = true) : RefreshTokenPersistence {
    var stored: String? = null
    private val deviceSecured = secured
    override fun isDeviceSecured() = deviceSecured
    override fun hasStored() = stored != null
    override fun store(token: String) { stored = token }
    override fun delete() { stored = null }
}

class RefreshTokenVaultTest {

    @Test
    fun `current is null before unlock even when a token is persisted`() {
        val store = FakePersistence().apply { stored = "persisted" }
        val vault = RefreshTokenVault(store)
        assertNull(vault.current())          // never read from disk without an unlock
        assertTrue(vault.hasStored())
    }

    @Test
    fun `store persists and caches in memory`() {
        val store = FakePersistence()
        val vault = RefreshTokenVault(store)
        vault.store("t1")
        assertEquals("t1", vault.current())
        assertEquals("t1", store.stored)
    }

    @Test
    fun `acceptUnlocked populates the in-memory session`() {
        val store = FakePersistence().apply { stored = "disk-token" }
        val vault = RefreshTokenVault(store)
        vault.acceptUnlocked("disk-token")
        assertEquals("disk-token", vault.current())
    }

    @Test
    fun `lock clears memory but keeps persisted token`() {
        val store = FakePersistence()
        val vault = RefreshTokenVault(store)
        vault.store("t1")
        vault.lock()
        assertNull(vault.current())
        assertTrue(vault.hasStored())
    }

    @Test
    fun `clear wipes memory and persistence`() {
        val store = FakePersistence()
        val vault = RefreshTokenVault(store)
        vault.store("t1")
        vault.clear()
        assertNull(vault.current())
        assertFalse(vault.hasStored())
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "*RefreshTokenVaultTest"`
Expected: FAIL — `RefreshTokenVault` unresolved.

- [ ] **Step 4: Implement the vault**

Create `RefreshTokenVault.kt`:

```kotlin
package com.hanmaum.dn.mobile.core.security

/**
 * Bridges the gated [RefreshTokenPersistence] and the running session. The
 * offline token only enters [current] after a successful unlock (or a fresh
 * store at login); silent token refresh reads [current] synchronously and never
 * prompts. On background the session calls [lock] to drop the in-memory copy.
 */
class RefreshTokenVault(private val persistence: RefreshTokenPersistence) {

    private var inMemory: String? = null

    /** In-memory token for this foreground session; null when locked. */
    fun current(): String? = inMemory

    fun hasStored(): Boolean = persistence.hasStored()

    fun isDeviceSecured(): Boolean = persistence.isDeviceSecured()

    /** Persist (free, no prompt) after login or token rotation, and cache it. */
    fun store(token: String) {
        persistence.store(token)
        inMemory = token
    }

    /** Cache a token just read via a biometric unlock. */
    fun acceptUnlocked(token: String) { inMemory = token }

    /** Drop the in-memory copy (on background); persisted token survives. */
    fun lock() { inMemory = null }

    /** Wipe both memory and persistent storage (logout). */
    fun clear() {
        inMemory = null
        persistence.delete()
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "*RefreshTokenVaultTest"`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenVault.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricRefreshStore.kt \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/security/RefreshTokenVaultTest.kt
git commit -m "feat(security): RefreshTokenVault in-memory session over gated store"
```

---

### Task 10: Route `TokenStorage` refresh token through the vault

**Files:**
- Modify: `composeApp/src/commonMain/.../core/data/repository/TokenStorageImpl.kt`
- Modify: `composeApp/src/commonMain/.../di/AppModule.kt`
- Modify: `composeApp/src/.../di/PlatformModule.{android,ios}.kt`

> `TokenStorage`'s interface is unchanged (still `saveRefreshToken/getRefreshToken/clear`), so `NetworkClient`, `SessionManager`, and `LoginViewModel` need no edits. Only the impl is re-pointed: access token + flags stay in L1 secure Settings; the refresh token goes to the vault.

- [ ] **Step 1: Inject the vault into `TokenStorageImpl`**

In `TokenStorageImpl.kt`, change the constructor and the refresh-token + clear methods:

```kotlin
package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.RefreshTokenVault
import com.russhwolf.settings.Settings

class TokenStorageImpl(
    private val settings: Settings,
    private val refreshVault: RefreshTokenVault,
) : TokenStorage {

    override fun saveAccessToken(token: String) {
        settings.putString(KEY_ACCESS, token)
    }

    override fun getAccessToken(): String? = settings.getStringOrNull(KEY_ACCESS)

    override fun saveRefreshToken(token: String?) {
        if (token != null) refreshVault.store(token) else refreshVault.clear()
    }

    // In-memory only: returns the token unlocked for this session, else null.
    override fun getRefreshToken(): String? = refreshVault.current()

    override fun clear() {
        settings.remove(KEY_ACCESS)
        refreshVault.clear()
        settings.remove(KEY_KEEP_SIGNED_IN)
        settings.remove(KEY_BIOMETRIC)
    }

    override fun setKeepSignedIn(value: Boolean) {
        settings.putBoolean(KEY_KEEP_SIGNED_IN, value)
    }

    override fun isKeepSignedIn(): Boolean = settings.getBoolean(KEY_KEEP_SIGNED_IN, true)

    override fun setBiometricEnabled(value: Boolean) {
        settings.putBoolean(KEY_BIOMETRIC, value)
    }

    override fun isBiometricEnabled(): Boolean = settings.getBoolean(KEY_BIOMETRIC, false)

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_KEEP_SIGNED_IN = "keep_signed_in"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
```

(The `KEY_REFRESH` constant is removed — refresh tokens no longer live in `Settings`.)

- [ ] **Step 2: Provide `BiometricRefreshStore` + `RefreshTokenVault` in DI**

In **both** `PlatformModule.android.kt` and `PlatformModule.ios.kt`, add the store binding (Android needs context, iOS no-arg):

Android:
```kotlin
import com.hanmaum.dn.mobile.core.security.BiometricRefreshStore
// ...
    single { BiometricRefreshStore(androidContext()) }
```

iOS:
```kotlin
import com.hanmaum.dn.mobile.core.security.BiometricRefreshStore
// ...
    single { BiometricRefreshStore() }
```

In `AppModule.kt`, add the vault and update the `TokenStorage` binding:

```kotlin
import com.hanmaum.dn.mobile.core.security.RefreshTokenVault
// ...
    single { RefreshTokenVault(get<com.hanmaum.dn.mobile.core.security.BiometricRefreshStore>()) }
    single<TokenStorage> { TokenStorageImpl(get(named("secure")), get()) }
```

- [ ] **Step 3: Fix the existing `TokenStorageImplTest`**

`TokenStorageImplTest` constructs `TokenStorageImpl(settings)`. Its refresh-token round-trip test now belongs to `RefreshTokenVaultTest`. Update `TokenStorageImplTest.kt`: pass a fake vault and delete the refresh-token assertions from it.

Replace the `storage(...)` helper and the refresh-token test:

```kotlin
import com.hanmaum.dn.mobile.core.security.RefreshTokenVault
import com.hanmaum.dn.mobile.core.security.RefreshTokenPersistence

private class NoopPersistence : RefreshTokenPersistence {
    var stored: String? = null
    override fun isDeviceSecured() = true
    override fun hasStored() = stored != null
    override fun store(token: String) { stored = token }
    override fun delete() { stored = null }
}

private fun storage(settings: MapSettings = MapSettings()) =
    TokenStorageImpl(settings, RefreshTokenVault(NoopPersistence()))
```

Delete the test `access and refresh tokens round-trip` (refresh is covered by `RefreshTokenVaultTest`); keep the access-token + flags tests but ensure they don't assert on `getRefreshToken`.

- [ ] **Step 4: Run all unit tests**

Run: `./gradlew :composeApp:testDevDebugUnitTest`
Expected: PASS (including `RefreshTokenVaultTest`, `SessionValidatorTest`, updated `TokenStorageImplTest`).

- [ ] **Step 5: Compile both targets**

Run: `./gradlew :composeApp:assembleDevDebug && DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/TokenStorageImpl.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt \
        composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/di/PlatformModule.android.kt \
        composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/di/PlatformModule.ios.kt \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/data/repository/TokenStorageImplTest.kt
git commit -m "feat(security): route offline refresh token through the gated vault"
```

---

### Task 11: Drive the lock screen from the vault (unlock = gated read)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt` (lock state `:70-80`, unlock effects `:100-126`, lock overlay `:321-337`)

> Today `locked` is gated on the opt-in `isBiometricEnabled()`. Now the offline token is *always* gated, so the lock must appear whenever there's a stored token to unlock, and the unlock action must be the gated read that populates `vault.current()`. After unlock we still run `sessionValidator.isSessionValid()`.

- [ ] **Step 1: Inject the vault + unlocker, recompute `locked`**

In `App.kt`, add imports:

```kotlin
import com.hanmaum.dn.mobile.core.security.RefreshTokenVault
import com.hanmaum.dn.mobile.core.security.RefreshTokenUnlocker
import com.hanmaum.dn.mobile.core.security.rememberRefreshTokenUnlocker
import com.hanmaum.dn.mobile.core.security.UnlockResult
```

Replace the lock-state block (`val tokenStorage = ...` through the `LifecycleEventEffect(Lifecycle.Event.ON_STOP)`) with:

```kotlin
        val tokenStorage = koinInject<TokenStorage>()
        val sessionManager = koinInject<SessionManager>()
        val sessionValidator = koinInject<SessionValidator>()
        val refreshVault = koinInject<RefreshTokenVault>()
        val unlocker = rememberRefreshTokenUnlocker()
        val lockScope = rememberCoroutineScope()
        // Locked whenever a stay-signed-in session has a gated token that isn't
        // yet unlocked into memory. (Biometric opt-in no longer gates this — the
        // token is always behind device auth.)
        var locked by remember {
            mutableStateOf(refreshVault.hasStored() && refreshVault.current() == null)
        }
        // Re-lock on background: drop the in-memory token and re-prompt next foreground.
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            if (refreshVault.hasStored()) {
                refreshVault.lock()
                locked = true
            }
        }
```

(Remove the now-unused `biometric`/`biometricEnabled` lock vars if they are not referenced elsewhere in `App.kt`; the Profile biometric *toggle* UI is unaffected.)

- [ ] **Step 2: Replace the auto-prompt + unlock effects**

Replace the `LaunchedEffect(locked)` auto-prompt and keep the global logout sink. New version:

```kotlin
            // Single global logout sink (unchanged).
            LaunchedEffect(Unit) {
                sessionManager.events.collect {
                    locked = false
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            // Auto-prompt the gated read whenever locked.
            LaunchedEffect(locked) {
                if (locked) {
                    when (val result = unlocker.unlock(strings.lockSubtitle)) {
                        is UnlockResult.Success -> {
                            refreshVault.acceptUnlocked(result.token)
                            locked = false
                            sessionValidator.isSessionValid() // dead token → logout sink → Login
                        }
                        UnlockResult.Empty -> {
                            // Nothing to unlock → go to login.
                            locked = false
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }; launchSingleTop = true
                            }
                        }
                        UnlockResult.Cancelled, UnlockResult.Failed -> {
                            // Stay locked; the LockScreen's retry button re-triggers.
                        }
                    }
                }
            }
```

- [ ] **Step 3: Update the `LockScreen` overlay actions**

Replace the lock overlay block (`if (locked) { LockScreen(...) }`) with:

```kotlin
            if (locked) {
                LockScreen(
                    onUnlock = {
                        lockScope.launch {
                            when (val result = unlocker.unlock(strings.lockSubtitle)) {
                                is UnlockResult.Success -> {
                                    refreshVault.acceptUnlocked(result.token)
                                    locked = false
                                    sessionValidator.isSessionValid()
                                }
                                UnlockResult.Empty -> {
                                    locked = false
                                    navController.navigate(LoginRoute) {
                                        popUpTo(0) { inclusive = true }; launchSingleTop = true
                                    }
                                }
                                UnlockResult.Cancelled, UnlockResult.Failed -> Unit
                            }
                        }
                    },
                    onUsePassword = {
                        // Deliberate sign-out → canonical pipeline; sink navigates to Login.
                        lockScope.launch { sessionManager.logout() }
                    },
                )
            }
```

- [ ] **Step 4: Compile + run unit tests**

Run: `./gradlew :composeApp:assembleDevDebug :composeApp:testDevDebugUnitTest && DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL, tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git commit -m "feat(security): lock screen unlocks the gated offline token (L2 flow)"
```

---

### Task 12: Lint, TODO gate, and full verification

**Files:** none (verification only).

- [ ] **Step 1: Lint + TODO grep (the CI gates)**

Run: `./gradlew :composeApp:lintDevDebug` → Expected: BUILD SUCCESSFUL.
Run: `grep -rn "TODO" composeApp/src` → Expected: no matches (exit non-zero).

- [ ] **Step 2: Android on-device E2E**

`./gradlew :composeApp:installDevDebug`. With the backend reachable: log in (Keep me signed in). Background the app → foreground → a biometric/passcode prompt appears → on success, data loads (no re-login). Cancel the prompt → stays locked. "Use password" → returns to Login. Confirm `adb shell run-as ... cat /data/data/com.hanmaum.dn.mobile/shared_prefs/dn_refresh_secure.xml` shows only base64 ciphertext.

- [ ] **Step 3: iOS on-device/simulator E2E**

Build/run per `tasks/lessons.md`. Enable Face ID enrollment (Simulator → Features → Face ID → Enrolled). Log in, background, foreground → Face ID prompt → Matching Face → data loads; Non-matching → stays locked. Terminate + relaunch (cold start) → Face ID prompt before Home.

- [ ] **Step 4: Verify silent refresh does NOT prompt**

While unlocked and active, let the access token expire (or set a short access-token TTL in Keycloak) and trigger a backend call → it refreshes silently with **no** biometric prompt (proves the in-memory session works). Only background→foreground re-prompts.

- [ ] **Step 5: Final commit / tag**

```bash
git tag l2-gated-refresh-token
```

---

## Self-Review notes (carried into execution)

- **Migration:** users upgrading from the plaintext build have a refresh token in old `SharedPreferences`/`NSUserDefaults`. Stage 1 reads from the *new* secure store, which is empty → they are routed to Login once (acceptable, one-time). No explicit migration code; document in the release notes. If a silent migration is required, add a pre-Task-3 step copying the old key then deleting it — **out of scope for "ship ASAP."**
- **Keystore invalidation:** changing device biometrics/passcode can invalidate the Android Keystore key / iOS access-control item → the gated read throws/returns null → treated as `Failed`/dead session → user re-logs in. This is correct fail-safe behaviour; verify it doesn't crash (the `runCatching` guards in Tasks 6/8 cover it).
- **API 24–29 Android:** uses the legacy `setUserAuthenticationValidityDurationSeconds(-1)` path (Task 6 Step 3) which still routes through `BiometricPrompt`+`CryptoObject`. Verify on an API 24 emulator.
- **iOS CF bridging (Task 7 Step 2):** the `toCFDictionary` helper is the single most toolchain-sensitive spot; budget a short iteration there.
- **Spec coverage:** L1 = Tasks 1–4; L2 split-token + gating + in-memory session + unified unlock = Tasks 5–11; verification = Task 12. The "device-credential fallback so stay-signed-in always works" decision is realized via `kSecAccessControlUserPresence` (iOS) and `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` (Android).
