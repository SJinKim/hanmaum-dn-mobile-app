package com.hanmaum.dn.mobile.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import kotlin.coroutines.resume

/**
 * Android half of the vault: an AES/GCM key in the AndroidKeyStore that the
 * system will only use inside an authenticated `BiometricPrompt`.
 *
 * `setUserAuthenticationRequired(true)` is what moves the decision out of the
 * app — the `Cipher` handed to `doFinal` is one the OS unlocked, so there is no
 * boolean in between to hook. `setInvalidatedByBiometricEnrollment(true)` then
 * destroys the key when a new fingerprint or face is enrolled, which is why
 * [VaultResult.Invalidated] exists: after that the ciphertext is unreadable by
 * anyone, including us, and the member has to switch Face ID on again.
 *
 * Class 3 (`BIOMETRIC_STRONG`) throughout — a crypto-bound key cannot be
 * unlocked by weak biometrics.
 *
 * Two layers rather than one: the Keystore key wraps a random data key, and the
 * data key encrypts the token. That is what makes [reseal] possible without a
 * second prompt — the rotated token is re-encrypted with the data key the just
 * finished prompt released, while the Keystore key keeps its per-use
 * authentication untouched. The alternative, a Keystore key with a time-based
 * validity window, is API 30+ and cannot be used with a `CryptoObject` at all.
 */
class AndroidBiometricVault(
    private val context: Context,
    /**
     * Null outside composition. Sealing and opening raise a `BiometricPrompt`,
     * which needs the hosting activity; clearing and the availability check do
     * not, and logout has to be able to clear the vault from a ViewModel.
     */
    private val activity: FragmentActivity? = null,
) : BiometricVault {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * The data key from the last prompt, held so [reseal] can write the rotated
     * token back. Dropped once used or the vault is cleared. It authorises a
     * write, never a read: the ciphertext it produces still needs a real
     * biometric match to be opened again.
     */
    private var authorised: SecretKey? = null

    /** Android has no per-app biometric permission, so it never reports DENIED. */
    override fun availability(): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }

    override fun hasSecret(): Boolean = prefs.contains(KEY_PAYLOAD) && prefs.contains(KEY_WRAPPED)

    override suspend fun seal(
        secret: String,
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): VaultResult {
        if (!isAvailable()) return VaultResult.Unavailable

        // A fresh key per sealing: the old ciphertext is being replaced anyway,
        // and this keeps a key that outlived its secret from lingering.
        deleteKey()
        val cipher = try {
            Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, createKey()) }
        } catch (e: Exception) {
            return VaultResult.Failed
        }

        return when (val prompt = prompt(cipher, title, subtitle, cancelLabel)) {
            is PromptOutcome.Ok -> try {
                // The prompt wrapped the data key; the token itself is encrypted
                // with that data key, one layer down.
                val dataKey = SecretKeySpec(ByteArray(DATA_KEY_BYTES).also(SecureRandom()::nextBytes), "AES")
                val wrapped = prompt.cipher.doFinal(dataKey.encoded)
                prefs.edit()
                    .putString(KEY_WRAPPED, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                    .putString(KEY_WRAPPED_IV, Base64.encodeToString(prompt.cipher.iv, Base64.NO_WRAP))
                    .apply()
                authorised = dataKey
                if (writePayload(dataKey, secret)) VaultResult.Success("") else VaultResult.Failed
            } catch (e: Exception) {
                VaultResult.Failed
            }
            is PromptOutcome.Error -> prompt.result
        }
    }

    override suspend fun open(title: String, subtitle: String, cancelLabel: String): VaultResult {
        if (!isAvailable()) return VaultResult.Unavailable
        val payload = prefs.getString(KEY_PAYLOAD, null) ?: return VaultResult.Empty
        val iv = prefs.getString(KEY_IV, null) ?: return VaultResult.Empty
        // Absent on an install that sealed under the single-layer scheme. There is
        // no key left that could read that ciphertext without a prompt-per-use, so
        // it reads as "not set up" and the member arms Face ID once more.
        val wrapped = prefs.getString(KEY_WRAPPED, null) ?: return VaultResult.Empty
        val wrappedIv = prefs.getString(KEY_WRAPPED_IV, null) ?: return VaultResult.Empty

        val cipher = try {
            Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    loadKey() ?: return VaultResult.Empty,
                    GCMParameterSpec(TAG_BITS, Base64.decode(wrappedIv, Base64.NO_WRAP)),
                )
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            // New biometric enrolled — the key is gone and the ciphertext is
            // scrap. Drop it so nothing keeps retrying against it.
            clear()
            return VaultResult.Invalidated
        } catch (e: Exception) {
            return VaultResult.Failed
        }

        return when (val prompt = prompt(cipher, title, subtitle, cancelLabel)) {
            is PromptOutcome.Ok -> try {
                val dataKey = SecretKeySpec(prompt.cipher.doFinal(Base64.decode(wrapped, Base64.NO_WRAP)), "AES")
                val plain = Cipher.getInstance(TRANSFORMATION).apply {
                    init(Cipher.DECRYPT_MODE, dataKey, GCMParameterSpec(TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)))
                }.doFinal(Base64.decode(payload, Base64.NO_WRAP))
                authorised = dataKey
                VaultResult.Success(plain.decodeToString())
            } catch (e: Exception) {
                VaultResult.Failed
            }
            is PromptOutcome.Error -> prompt.result
        }
    }

    override suspend fun reseal(secret: String): VaultResult {
        val dataKey = authorised ?: return VaultResult.Failed
        authorised = null
        return if (writePayload(dataKey, secret)) VaultResult.Success("") else VaultResult.Failed
    }

    /** Encrypts the token with the data key. No Keystore, so no prompt. */
    private fun writePayload(dataKey: SecretKey, secret: String): Boolean = try {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, dataKey) }
        val ciphertext = cipher.doFinal(secret.encodeToByteArray())
        prefs.edit()
            .putString(KEY_PAYLOAD, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
        true
    } catch (e: Exception) {
        false
    }

    override fun clear() {
        authorised = null
        prefs.edit()
            .remove(KEY_PAYLOAD).remove(KEY_IV)
            .remove(KEY_WRAPPED).remove(KEY_WRAPPED_IV)
            .apply()
        deleteKey()
    }

    private sealed interface PromptOutcome {
        data class Ok(val cipher: Cipher) : PromptOutcome
        data class Error(val result: VaultResult) : PromptOutcome
    }

    private suspend fun prompt(
        cipher: Cipher,
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): PromptOutcome = withContext<PromptOutcome>(Dispatchers.Main) {
        val host = activity ?: return@withContext PromptOutcome.Error(VaultResult.Unavailable)
        suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                host,
                ContextCompat.getMainExecutor(host),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val unlocked = result.cryptoObject?.cipher
                        if (cont.isActive) {
                            cont.resume(
                                if (unlocked != null) PromptOutcome.Ok(unlocked)
                                else PromptOutcome.Error(VaultResult.Failed),
                            )
                        }
                    }

                    override fun onAuthenticationError(code: Int, msg: CharSequence) {
                        val cancelled = code == BiometricPrompt.ERROR_USER_CANCELED ||
                            code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            code == BiometricPrompt.ERROR_CANCELED
                        if (cont.isActive) {
                            cont.resume(
                                PromptOutcome.Error(
                                    if (cancelled) VaultResult.Cancelled else VaultResult.Failed,
                                ),
                            )
                        }
                    }
                    // onAuthenticationFailed = one mismatch; the prompt stays up.
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancelLabel)
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .build()
            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun loadKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun deleteKey() {
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            // Nothing to delete, or the keystore is unavailable; either way there
            // is no key left to protect.
        }
    }

    private companion object {
        const val PREFS_NAME = "dn_biometric_vault"
        const val KEY_PAYLOAD = "vault_payload"
        const val KEY_IV = "vault_iv"
        const val KEY_WRAPPED = "vault_wrapped_key"
        const val KEY_WRAPPED_IV = "vault_wrapped_key_iv"
        const val DATA_KEY_BYTES = 32
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "dn_biometric_vault_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}

@Composable
actual fun rememberBiometricVault(): BiometricVault {
    val activity = LocalActivity.current as FragmentActivity
    return remember { AndroidBiometricVault(activity.applicationContext, activity) }
}
