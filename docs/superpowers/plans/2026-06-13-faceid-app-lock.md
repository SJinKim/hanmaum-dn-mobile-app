# Face ID / Biometric App Lock — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an opt-in biometric app lock (Face ID / Touch ID / fingerprint) that gates the app on cold launch and on resume from background, toggleable from Profile.

**Architecture:** A hand-rolled `expect/actual BiometricAuthenticator` (Android `androidx.biometric.BiometricPrompt`, iOS `LocalAuthentication`), obtained via a `@Composable rememberBiometricAuthenticator()`. A settings-backed `biometricEnabled` flag on `TokenStorage`. A lock gate in `App.kt` shows a `LockScreen` over the NavHost when enabled + a session exists, re-locking on `ON_STOP`. UI-gate model (reveals the already-stored session; no token crypto-binding).

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10.0, androidx.biometric, iOS LocalAuthentication (Kotlin/Native interop), Koin, multiplatform-settings, kotlinx-coroutines.

**Spec:** `docs/superpowers/specs/2026-06-13-faceid-app-lock-design.md`

---

## File Structure

**Create:**
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.kt` — `BiometricResult` enum + `expect class BiometricAuthenticator` + `@Composable expect fun rememberBiometricAuthenticator()`.
- `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.android.kt` — Android actual.
- `composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.ios.kt` — iOS actual.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/LockScreen.kt` — the lock overlay.

**Modify:**
- `gradle/libs.versions.toml` — add `androidx-biometric` version + lib.
- `composeApp/build.gradle.kts` — add biometric to `androidMain.dependencies`.
- `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/MainActivity.kt` — `ComponentActivity` → `FragmentActivity`.
- `iosApp/iosApp/Info.plist` — `NSFaceIDUsageDescription`.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/repository/TokenStorage.kt` — `setBiometricEnabled`/`isBiometricEnabled`.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/TokenStorageImpl.kt` — implement them.
- `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/data/repository/TokenStorageImplTest.kt` — cover the flag.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStrings.kt` — lock strings (interface + En/Ko/De).
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt` — lock gate + pass biometric state to Profile.
- `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt` — App-lock toggle row.

**Build/test commands (per `tasks/lessons.md`):** use `:composeApp:assembleDevDebug`, `:composeApp:testDevDebugUnitTest`. iOS compile check: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`.

---

## Task 1: Add androidx.biometric dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add version + library to `gradle/libs.versions.toml`**

Under `[versions]`, after the line `androidx-appcompat = "1.7.1"`, add:
```toml
androidx-biometric = "1.1.0"
```
Under `[libraries]`, after the line `androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "androidx-appcompat" }`, add:
```toml
androidx-biometric = { module = "androidx.biometric:biometric", version.ref = "androidx-biometric" }
```

- [ ] **Step 2: Add the dependency to `composeApp/build.gradle.kts`**

In the `androidMain.dependencies { … }` block, after `implementation(libs.androidx.activity.compose)`, add:
```kotlin
            implementation(libs.androidx.biometric)
```
(`androidx.biometric:1.1.0` transitively provides `androidx.fragment`, giving `FragmentActivity`.)

- [ ] **Step 3: Verify it resolves/compiles**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "build(android): add androidx.biometric dependency"
```

---

## Task 2: Common biometric abstraction (expect)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.kt`

- [ ] **Step 1: Create the file**
```kotlin
package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable

/** Outcome of a biometric prompt. */
enum class BiometricResult {
    SUCCESS,      // user authenticated
    FAILED,       // hardware/error or repeated mismatch
    CANCELLED,    // user dismissed the prompt
    UNAVAILABLE,  // no biometric hardware / nothing enrolled
}

/**
 * UI-gate biometric authenticator. Reveals the already-stored session on success;
 * it does not crypto-bind tokens. Obtain it inside Compose via
 * [rememberBiometricAuthenticator] (Android needs the hosting FragmentActivity).
 */
expect class BiometricAuthenticator {
    /** True only when biometric hardware is present AND something is enrolled. */
    fun isAvailable(): Boolean

    /** Shows the system biometric prompt and suspends until it resolves. */
    suspend fun authenticate(
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): BiometricResult
}

/** Creates a [BiometricAuthenticator] bound to the current platform UI context. */
@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
```

- [ ] **Step 2: Commit** (compiles once actuals exist — Task 3/4; commit together is fine, but commit now to checkpoint the contract)
```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.kt
git commit -m "feat(security): biometric authenticator contract (expect)"
```
> Note: the project won't fully compile until Tasks 3 and 4 add the actuals. Run the build at the end of Task 4.

---

## Task 3: Android actual + FragmentActivity

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/MainActivity.kt`
- Create: `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.android.kt`

- [ ] **Step 1: Change MainActivity to FragmentActivity**

Replace the contents of `MainActivity.kt` with:
```kotlin
package com.hanmaum.dn.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity

// FragmentActivity (extends ComponentActivity) is required by androidx.biometric's
// BiometricPrompt. setContent / enableEdgeToEdge are unaffected.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}
```

- [ ] **Step 2: Create the Android actual**

`composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.android.kt`:
```kotlin
package com.hanmaum.dn.mobile.core.security

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

actual class BiometricAuthenticator(private val activity: FragmentActivity) {

    actual fun isAvailable(): Boolean =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    actual suspend fun authenticate(
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): BiometricResult = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(BiometricResult.SUCCESS)
                    }
                    override fun onAuthenticationError(code: Int, msg: CharSequence) {
                        val cancelled = code == BiometricPrompt.ERROR_USER_CANCELED ||
                            code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            code == BiometricPrompt.ERROR_CANCELED
                        if (cont.isActive) {
                            cont.resume(if (cancelled) BiometricResult.CANCELLED else BiometricResult.FAILED)
                        }
                    }
                    // onAuthenticationFailed = a single mismatch; not terminal, ignore.
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancelLabel)
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .build()
            prompt.authenticate(info)
        }
    }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val activity = LocalActivity.current as FragmentActivity
    return remember { BiometricAuthenticator(activity) }
}
```

- [ ] **Step 3: Verify Android compiles**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL. (iOS still missing its actual — that's Task 4.)

- [ ] **Step 4: Commit**
```bash
git add composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/MainActivity.kt \
        composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.android.kt
git commit -m "feat(security): android biometric actual via BiometricPrompt"
```

---

## Task 4: iOS actual + Info.plist

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.ios.kt`
- Modify: `iosApp/iosApp/Info.plist`

- [ ] **Step 1: Create the iOS actual**

`composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.ios.kt`:
```kotlin
package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

actual class BiometricAuthenticator {

    actual fun isAvailable(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)

    actual suspend fun authenticate(
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): BiometricResult {
        val context = LAContext()
        context.localizedCancelTitle = cancelLabel
        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) {
            return BiometricResult.UNAVAILABLE
        }
        return suspendCancellableCoroutine { cont ->
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = subtitle,
            ) { success, error ->
                val outcome = when {
                    success -> BiometricResult.SUCCESS
                    error?.code == LAErrorUserCancel -> BiometricResult.CANCELLED
                    else -> BiometricResult.FAILED
                }
                if (cont.isActive) cont.resume(outcome)
            }
        }
    }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { BiometricAuthenticator() }
```
> `title` is unused on iOS (the system Face ID sheet has no title) but kept for a common signature.

- [ ] **Step 2: Add the Face ID usage string to `iosApp/iosApp/Info.plist`**

Inside the top-level `<dict>` (e.g. after the `ITSAppUsesNonExemptEncryption` pair), add:
```xml
    <key>NSFaceIDUsageDescription</key>
    <string>앱 잠금을 해제하기 위해 Face ID를 사용합니다.</string>
```

- [ ] **Step 3: Verify both targets compile**

Run: `./gradlew :composeApp:assembleDevDebug`
Run: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
Expected: both BUILD SUCCESSFUL (the expect now has both actuals).

- [ ] **Step 4: Commit**
```bash
git add composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/security/BiometricAuthenticator.ios.kt \
        iosApp/iosApp/Info.plist
git commit -m "feat(security): ios biometric actual via LocalAuthentication"
```

---

## Task 5: Persist the biometric-enabled flag (TDD)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/repository/TokenStorage.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/TokenStorageImpl.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/data/repository/TokenStorageImplTest.kt`

- [ ] **Step 1: Add failing test** (append inside `TokenStorageImplTest` class, before the final `}`)
```kotlin
    @Test
    fun `isBiometricEnabled defaults to false`() {
        assertFalse(storage().isBiometricEnabled())
    }

    @Test
    fun `setBiometricEnabled true round-trips`() {
        val settings = MapSettings()
        storage(settings).setBiometricEnabled(true)
        assertTrue(storage(settings).isBiometricEnabled())
    }

    @Test
    fun `clear resets biometricEnabled to false`() {
        val settings = MapSettings()
        val s = storage(settings)
        s.setBiometricEnabled(true)
        s.clear()
        assertFalse(storage(settings).isBiometricEnabled())
    }
```

- [ ] **Step 2: Run — expect failure** (unresolved `setBiometricEnabled`/`isBiometricEnabled`)

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.core.data.repository.TokenStorageImplTest"`
Expected: FAIL (compile error: unresolved reference).

- [ ] **Step 3: Add to the `TokenStorage` interface** (after `isKeepSignedIn(): Boolean`)
```kotlin

    /** Whether the biometric app lock is enabled. Defaults to false (opt-in). */
    fun setBiometricEnabled(value: Boolean)
    fun isBiometricEnabled(): Boolean
```

- [ ] **Step 4: Implement in `TokenStorageImpl`**

Add the two methods (after `isKeepSignedIn`):
```kotlin
    override fun setBiometricEnabled(value: Boolean) {
        settings.putBoolean(KEY_BIOMETRIC, value)
    }

    override fun isBiometricEnabled(): Boolean = settings.getBoolean(KEY_BIOMETRIC, false)
```
Add to the `clear()` body (after `settings.remove(KEY_KEEP_SIGNED_IN)`):
```kotlin
        settings.remove(KEY_BIOMETRIC)
```
Add to the `companion object`:
```kotlin
        private const val KEY_BIOMETRIC = "biometric_enabled"
```

- [ ] **Step 5: Run — expect pass**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.core.data.repository.TokenStorageImplTest"`
Expected: PASS (all tests).

- [ ] **Step 6: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/repository/TokenStorage.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/data/repository/TokenStorageImpl.kt \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/data/repository/TokenStorageImplTest.kt
git commit -m "feat(security): persist biometric-enabled flag in TokenStorage"
```

---

## Task 6: Lock-screen i18n strings

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStrings.kt`

- [ ] **Step 1: Add to the `interface AppStrings`** (after `themeDark: String`)
```kotlin
    val profileAppLock: String
    val appLockUnavailable: String
    val lockTitle: String
    val lockSubtitle: String
    val lockUnlockButton: String
    val lockUsePassword: String
```

- [ ] **Step 2: Add to `EnStrings`** (after `themeDark = "Dark"`)
```kotlin
    override val profileAppLock = "App Lock (Face ID / Touch ID)"
    override val appLockUnavailable = "No biometrics enrolled on this device"
    override val lockTitle = "Locked"
    override val lockSubtitle = "Unlock DN App to continue"
    override val lockUnlockButton = "Unlock"
    override val lockUsePassword = "Use password instead"
```

- [ ] **Step 3: Add to `KoStrings`** (after `themeDark = "다크"`)
```kotlin
    override val profileAppLock = "앱 잠금 (Face ID / Touch ID)"
    override val appLockUnavailable = "기기에 등록된 생체 인증이 없습니다"
    override val lockTitle = "잠금"
    override val lockSubtitle = "계속하려면 DN 앱 잠금을 해제하세요"
    override val lockUnlockButton = "잠금 해제"
    override val lockUsePassword = "비밀번호로 로그인"
```

- [ ] **Step 4: Add to `DeStrings`** (after `themeDark = "Dunkel"`)
```kotlin
    override val profileAppLock = "App-Sperre (Face ID / Touch ID)"
    override val appLockUnavailable = "Keine Biometrie auf diesem Gerät eingerichtet"
    override val lockTitle = "Gesperrt"
    override val lockSubtitle = "Entsperre die DN App, um fortzufahren"
    override val lockUnlockButton = "Entsperren"
    override val lockUsePassword = "Stattdessen Passwort verwenden"
```

- [ ] **Step 5: Verify (interface forces parity)**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.core.i18n.AppStringsTest"`
Expected: PASS.

- [ ] **Step 6: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStrings.kt
git commit -m "feat(security): app-lock i18n strings (en/ko/de)"
```

---

## Task 7: LockScreen composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/LockScreen.kt`

- [ ] **Step 1: Create the file**
```kotlin
package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import hanmaumdnapp.composeapp.generated.resources.Res
import hanmaumdnapp.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

/** Full-screen lock overlay shown while the biometric app lock is engaged. */
@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    onUsePassword: () -> Unit,
) {
    val strings = LocalStrings.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.height(96.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = strings.lockTitle,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.lockSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text(strings.lockUnlockButton, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onUsePassword) {
                Text(strings.lockUsePassword, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/components/LockScreen.kt
git commit -m "feat(security): add LockScreen overlay"
```

---

## Task 8: Lock gate in App.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`

Context: `App()` already injects `localeRepo`/`themeRepo` and hoists `locale`/`themeMode`, then renders `AppTheme { Scaffold { Box { NavHost(...) ; FloatingPillNav } } }`. We add a `TokenStorage` injection, lock state, a lifecycle re-lock, and overlay the `LockScreen` while locked.

- [ ] **Step 1: Add imports** (with the other imports at the top of `App.kt`)
```kotlin
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.presentation.components.LockScreen
import com.hanmaum.dn.mobile.core.security.BiometricResult
import com.hanmaum.dn.mobile.core.security.rememberBiometricAuthenticator
import kotlinx.coroutines.launch
```

- [ ] **Step 2: Inject TokenStorage + biometric + lock state**

Find:
```kotlin
        val themeRepo = koinInject<ThemeRepository>()
        var themeMode by remember { mutableStateOf(themeRepo.getThemeMode()) }
```
Add immediately below:
```kotlin
        val tokenStorage = koinInject<TokenStorage>()
        val biometric = rememberBiometricAuthenticator()
        val lockScope = rememberCoroutineScope()
        // Hoisted so the Profile switch reflects changes immediately (Task 9).
        var biometricEnabled by remember { mutableStateOf(tokenStorage.isBiometricEnabled()) }
        // Locked when the lock is enabled and there is a session to protect.
        var locked by remember {
            mutableStateOf(tokenStorage.isBiometricEnabled() && tokenStorage.getAccessToken() != null)
        }
        // Re-lock when the app goes to the background so the next foreground re-prompts.
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            if (tokenStorage.isBiometricEnabled() && tokenStorage.getAccessToken() != null) {
                locked = true
            }
        }
```

- [ ] **Step 3: Auto-prompt while locked + render the overlay**

The `AppTheme(themeMode = themeMode) { … }` body currently begins with `val navController = rememberNavController()`. Immediately inside `AppTheme { … }`, before `val navController`, add the prompt effect:
```kotlin
            LaunchedEffect(locked) {
                if (locked) {
                    when (biometric.authenticate(strings.lockTitle, strings.lockSubtitle, strings.lockUsePassword)) {
                        BiometricResult.SUCCESS -> locked = false
                        else -> { /* stay locked; user retries or uses password */ }
                    }
                }
            }
```
Then, at the very end of the `AppTheme { … }` body (after the `Scaffold { … }` closes, still inside `AppTheme`), add the overlay:
```kotlin
            if (locked) {
                LockScreen(
                    onUnlock = {
                        lockScope.launch {
                            if (biometric.authenticate(strings.lockTitle, strings.lockSubtitle, strings.lockUsePassword) == BiometricResult.SUCCESS) {
                                locked = false
                            }
                        }
                    },
                    onUsePassword = {
                        tokenStorage.clear()
                        locked = false
                        navController.navigate(LoginRoute) { popUpTo(0) { inclusive = true } }
                    },
                )
            }
```
> `navController` is in scope at that point (declared earlier in the `AppTheme` body). The overlay sits above the `Scaffold`/`NavHost` because it is composed after it in the same `Box`-less `AppTheme` content — if `AppTheme`'s content is not a layout container, wrap the existing `Scaffold(...)` and this `if (locked)` block together in a `androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) { … }` so the overlay draws on top. Add `import androidx.compose.foundation.layout.Box` and `import androidx.compose.foundation.layout.fillMaxSize` if not present.

- [ ] **Step 4: Verify compile**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git commit -m "feat(security): biometric lock gate on launch and resume"
```

---

## Task 9: App-lock toggle in Profile

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt`

- [ ] **Step 1: App.kt — pass biometric state into ProfileScreen**

The `biometricEnabled` state was hoisted in Task 8 Step 2. The `ProfileScreen(...)` call currently passes `currentTheme`/`onThemeChange`; add three more args to that call (after `onThemeChange = …,`):
```kotlin
                            biometricEnabled = biometricEnabled,
                            biometricAvailable = biometric.isAvailable(),
                            onBiometricToggle = { enable ->
                                if (enable) {
                                    lockScope.launch {
                                        if (biometric.authenticate(strings.lockTitle, strings.lockSubtitle, strings.lockUsePassword) == BiometricResult.SUCCESS) {
                                            tokenStorage.setBiometricEnabled(true)
                                            biometricEnabled = true
                                        }
                                    }
                                } else {
                                    tokenStorage.setBiometricEnabled(false)
                                    biometricEnabled = false
                                }
                            },
```
Enabling requires a successful biometric (proves the user can pass the check); disabling is immediate.

- [ ] **Step 2: ProfileScreen — add params**

Add imports:
```kotlin
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
```
Extend the public `ProfileScreen(...)` signature (after `onThemeChange`):
```kotlin
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
```
Pass into `ProfileViewContent(...)`:
```kotlin
                            biometricEnabled   = biometricEnabled,
                            biometricAvailable = biometricAvailable,
                            onBiometricToggle  = onBiometricToggle,
```
Extend `ProfileViewContent(...)` signature (after `onThemeChange`):
```kotlin
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
```

- [ ] **Step 3: ProfileScreen — render the toggle row** (immediately after the Theme `Card { … }` block in the "Account Preferences" section, before the `Spacer(Modifier.height(28.dp))` that precedes the quote box)
```kotlin
        Spacer(Modifier.height(12.dp))
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = MaterialTheme.shapes.large,
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = strings.profileAppLock,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!biometricAvailable) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text  = strings.appLockUnavailable,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = onBiometricToggle,
                    enabled = biometricAvailable,
                )
            }
        }
```

- [ ] **Step 4: Verify compile**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/profile/presentation/ProfileScreen.kt
git commit -m "feat(security): app-lock toggle in Profile"
```

---

## Task 10: Verification

- [ ] **Step 1: Unit tests + lint**

Run: `./gradlew :composeApp:testDevDebugUnitTest`
Expected: PASS (incl. new `TokenStorageImplTest` cases + `AppStringsTest`).
Run: `./gradlew lint` → no new errors. `grep -rn "TODO" composeApp/src` → none.

- [ ] **Step 2: iOS build + simulator manual test**
```bash
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/dnbuild CODE_SIGNING_ALLOWED=NO
SIM=$(xcrun simctl list devices available | grep -oE 'iPhone 1[5-7][^(]*\([0-9A-F-]{36}\)' | grep -oE '[0-9A-F-]{36}' | head -1)
xcrun simctl boot "$SIM" 2>/dev/null; sleep 6
xcrun simctl install "$SIM" /tmp/dnbuild/Build/Products/Debug-iphonesimulator/HanmaumDnApp.app
xcrun simctl launch "$SIM" com.hanmaum.dn.mobile.HanmaumDnApp
```
In the booted simulator: Features → Face ID → **Enrolled**. Log in, go to Profile, enable App Lock (Features → Face ID → **Matching Face** when prompted). Background the app and re-open → LockScreen appears → Matching Face unlocks. Test **Non-matching Face** keeps it locked; "Use password" returns to Login. Disable the toggle → no lock on next launch.

- [ ] **Step 3: Android manual test (optional, emulator)**

With an emulator that has a fingerprint enrolled: enable App Lock, background/foreground, `adb -e emu finger touch 1` to satisfy the prompt.

- [ ] **Step 4: Final acceptance** — confirm against spec §4: toggle opt-in + disabled when unavailable, enabling requires a biometric, lock on launch + resume, "Use password" logs out, flag cleared on logout.

- [ ] **Step 5: Push (only when the user asks)**
```bash
git push -u origin fix/ministry-and-auto-refresh
```
